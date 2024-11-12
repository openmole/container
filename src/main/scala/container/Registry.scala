/**
 * Copyright (C) 2017 Jonathan Passerat-Palmbach
 * Copyright (C) 2017 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package container

import better.files.{ File => BFile, _ }
import java.io.{ File, _ }
import java.util.concurrent.{ Callable, ThreadPoolExecutor, TimeUnit, TimeoutException }
import java.util.zip.{ GZIPInputStream, GZIPOutputStream }
import java.net.URI

import container.OCI.{ ConfigurationData, ManifestData }
import org.apache.http.{ Header, HttpHost, HttpRequest, HttpResponse }
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.{ HttpGet, HttpHead, HttpUriRequest, RequestBuilder }
import org.apache.http.impl.client.{ DefaultHttpRequestRetryHandler, HttpClients, LaxRedirectStrategy }
import org.apache.http.impl.conn.BasicHttpClientConnectionManager
import DockerMetadata._
import io.circe.syntax._
//import io.circe.generic.extras.auto._
import io.circe.generic.auto._
import io.circe.jawn.decode
import io.circe.parser._
import squants.time._
import cats.implicits._
import io.circe.Json
import org.apache.http.protocol.HttpContext

import container.Status._

// A remplacer ...
class UserBadDataError(exception: Throwable, val message: String) extends Exception(message, exception) {
  def this(message: String) = {
    this(null, message)
  }
  def this(e: Throwable) = {
    this(e, null)
  }
}
// ...

// ---------
object NetworkService {
  case class HttpHost(hostURI: String)
  object HttpHost {
    def toString(host: HttpHost) = host.hostURI
  }
}

class NetworkService(val httpProxy: Option[HttpHost])

case class Err(msg: String):
  def +(o: Err) = Err(msg + o.msg)

case class RegistryImage(
  name: String,
  tag: String = "latest",
  registry: String = "https://registry-1.docker.io",
  command: Seq[String] = Seq())

object Stream:
  def copy(inputStream: InputStream, outputStream: OutputStream) =
    val DefaultBufferSize = 16 * 1024
    val buffer = new Array[Byte](DefaultBufferSize)
    Iterator.continually(inputStream.read(buffer)).takeWhile(_ != -1).foreach { outputStream.write(buffer, 0, _) }

object Registry {

  // ...
  // type FileInfo = (External.DeployedFile, File)
  // type VolumeInfo = (File, String)
  type MountPoint = (String, String)
  type ContainerId = String
  // ...

  def content(response: HttpResponse) =
    scala.io.Source.fromInputStream(response.getEntity.getContent).mkString

  object HTTP {

    def redirectStrategy(preventGetHeaderForward: Boolean) = new LaxRedirectStrategy() {
      override def getRedirect(request: HttpRequest, response: HttpResponse, context: HttpContext): HttpUriRequest = {
        val uri = this.getLocationURI(request, response, context)
        val method = request.getRequestLine.getMethod
        val redirected = if (method.equalsIgnoreCase("HEAD")) new HttpHead(uri)
        else if (method.equalsIgnoreCase("GET")) {
          if (preventGetHeaderForward)
            new HttpGet(uri) {
              override def addHeader(header: Header): Unit = {}
              override def addHeader(name: ContainerId, value: ContainerId): Unit = {}
              override def setHeader(header: Header): Unit = {}
              override def setHeader(name: ContainerId, value: ContainerId): Unit = {}
              override def setHeaders(headers: Array[Header]): Unit = {}
            }
          else new HttpGet(uri)
        } else {
          val status = response.getStatusLine.getStatusCode
          (if (status == 307) RequestBuilder.copy(request).setUri(uri).build
          else new HttpGet(uri)).asInstanceOf[HttpUriRequest]
        }

        redirected
      }
    }

    def builder(preventGetHeaderForward: Boolean = false) =
      HttpClients.custom().setConnectionManager(new BasicHttpClientConnectionManager()).setRedirectStrategy(redirectStrategy(preventGetHeaderForward))
    /*
    def httpProxyAsHost(implicit networkService: NetworkService): Option[HttpHost] =
      networkService.httpProxy.map { host ⇒ HttpHost.create(NetworkService.HttpHost.toString(host)) }
*/
    def client(proxy: Option[HttpHost], preventGetHeaderForward: Boolean = false) =
      proxy match
        case Some(httpHost: HttpHost) ⇒ builder(preventGetHeaderForward = preventGetHeaderForward).setProxy(httpHost).build()
        case _ ⇒ builder(preventGetHeaderForward = preventGetHeaderForward).build()

    def execute[T](get: HttpGet, proxy: Option[HttpHost], checkError: Boolean = true, preventGetHeaderForward: Boolean = false)(f: HttpResponse ⇒ T) = {
      val response = client(proxy = proxy, preventGetHeaderForward = preventGetHeaderForward).execute(get)

      if (checkError && response.getStatusLine.getStatusCode >= 300)
        throw new UserBadDataError(s"Docker registry responded with $response to the query $get, content is ${response.getEntity.getContent.toString}")
      try f(response)
      finally response.close()
    }

  }

  import HTTP._

  // FIXME should integrate File?
  sealed trait LayerElement
  final case class Layer(digest: String) extends LayerElement
  final case class LayerConfig(digest: String) extends LayerElement
  case class Manifest(value: ImageManifestV2Schema1, image: RegistryImage)

  object Token {

    case class AuthenticationRequest(scheme: String, realm: String, service: String, scope: String)
    case class Token(scheme: String, token: String)

    def withToken(url: String, timeout: Time, proxy: Option[HttpHost], headers: Seq[(String, String)] = Seq()) =
      val get = new HttpGet(url)
      get.setConfig(RequestConfig.custom().setConnectTimeout(timeout.millis.toInt).setConnectionRequestTimeout(timeout.millis.toInt).build())
      headers.foreach: (h, c) =>
        get.addHeader(h, c)

      val authenticationRequest = authentication(get, proxy = proxy)

      val t = token(authenticationRequest, proxy = proxy)

      val request = new HttpGet(url)
      request.addHeader("Authorization", s"${t.scheme} ${t.token}")
      request.setConfig(RequestConfig.custom().setConnectTimeout(timeout.millis.toInt).setConnectionRequestTimeout(timeout.millis.toInt).build())
      request

    def authentication(get: HttpGet, proxy: Option[HttpHost]) =
      execute(get, proxy = proxy, checkError = false) { response ⇒
        Option(response.getFirstHeader("Www-Authenticate")).map(_.getValue).map {
          a ⇒
            val Array(scheme, rest) = a.split(" ")
            val map =
              rest.split(",").map {
                l ⇒
                  val kv = l.trim.split("=")
                  kv(0) → kv(1).stripPrefix("\"").stripSuffix("\"")
              }.toMap
            AuthenticationRequest(scheme, map("realm"), map("service"), map("scope"))
        }.getOrElse(throw new RuntimeException(s"Failed to authentication on the docker registry, response does not contain www-authenticate header: ${response}"))
      }

    def token(authenticationRequest: AuthenticationRequest, proxy: Option[HttpHost]): Token = {
      val tokenRequest = s"${authenticationRequest.realm}?service=${authenticationRequest.service}&scope=${authenticationRequest.scope}"

      val get = new HttpGet(tokenRequest)
      execute(get, proxy = proxy) { response ⇒
        val responseContent = content(response)

        try {
          val tokenRes = for {
            parsed ← parse(responseContent)
            token ← parsed.hcursor.get[String]("token")
          } yield Token(authenticationRequest.scheme, token)
          tokenRes.toTry.get
        } catch {
          case t: Throwable => throw new IOException(s"Failed to obtain authentication token from docker registry, response to query $get was $responseContent.", t)
        }
      }
    }

  }

  def baseURL(image: RegistryImage): String =
    val path = if (image.name.contains("/")) image.name else s"library/${image.name}"
    s"${image.registry}/v2/$path"

  def download(url: String, timeout: Time, proxy: Option[HttpHost], headers: Seq[(String, String)] = Seq()): String =
    val httpResponse =
      val request = Token.withToken(url, timeout, proxy = proxy, headers)
      headers.foreach: (h, c) =>
        request.addHeader(h, c)

      client(proxy = proxy, preventGetHeaderForward = true).execute(request)

    if httpResponse.getStatusLine.getStatusCode >= 300
    then throw new UserBadDataError(s"Docker registry responded with $httpResponse to query of $url")
    content(httpResponse)


  def downloadManifest(image: RegistryImage, timeout: Time, proxy: Option[HttpHost], headers: Seq[(String, String)] = Seq()): String =
    val url =  s"${baseURL(image)}/manifests/${image.tag}"
    download(url, timeout, proxy, headers)

  def decodeConfig(configContent: String) = decode[ImageJSON](configContent).toTry
  def decodeTopLevelManifest(manifestContent: String) = decode[List[TopLevelImageManifest]](manifestContent).map(_.head).toTry
  def decodeManifest(manifestContent: String): util.Try[ImageManifest] =
    val parsed = parse(manifestContent).toTry.get

    parsed.hcursor.get[String]("mediaType").toTry.get match
      case "application/vnd.oci.image.index.v1+json" => decode[ImageManifestV2Schema1](manifestContent).toTry
      case "application/vnd.docker.distribution.manifest.list.v2+json" => decode[ImageManifestV2Schema2List](manifestContent).toTry
      case "application/vnd.docker.distribution.manifest.v2+json" => decode[ImageManifestV2Schema2](manifestContent).toTry
      case other => throw UserBadDataError(s"Unknown media type $other in manifest:\n${manifestContent}")

  object Config:
    def workDirectory(config: ImageJSON) = config.config.flatMap(_.WorkingDir) orElse config.container_config.flatMap(_.WorkingDir)
    def env(config: ImageJSON) = config.config.flatMap(_.Env) orElse config.container_config.flatMap(_.Env)

  def layers(manifest: ImageManifestV2Schema1): Seq[Layer] =
    for {
      fsLayers ← manifest.fsLayers.toSeq
      fsLayer ← fsLayers
    } yield Layer(fsLayer.blobSum)

  def downloadBlob(image: RegistryImage, layer: Layer, file: BFile, timeout: Time, proxy: Option[HttpHost]): Unit =
    val url = s"""${baseURL(image)}/blobs/${layer.digest}"""
    execute(Token.withToken(url, timeout, proxy = proxy), preventGetHeaderForward = true, proxy = proxy): response ⇒
      val os = file.newOutputStream
      try Stream.copy(new GZIPInputStream(response.getEntity.getContent), os)
      finally os.close()

}

