package container

import better.files.{File => BFile, _}
import java.io.{File, _}
import java.util.concurrent.{Callable, ThreadPoolExecutor, TimeUnit, TimeoutException}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}
import java.net.URI

import container.OCI.{ConfigurationData, ManifestData}
import org.apache.http.{Header, HttpHost, HttpRequest, HttpResponse}
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.{HttpGet, HttpHead, HttpUriRequest, RequestBuilder}
import org.apache.http.impl.client.{DefaultHttpRequestRetryHandler, HttpClients, LaxRedirectStrategy}
import org.apache.http.impl.conn.BasicHttpClientConnectionManager
import DockerMetadata._
import io.circe.syntax._
import io.circe.generic.extras.auto._
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
  def this(e: Throwable) {
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
// ----------


//
case class Err(msg: String) {
  def +(o: Err) = Err(msg + o.msg)
}
//

//
sealed trait ContainerImage

case class DockerImage(imageName: String,
                       tag: String = "latest",
                       registry: String = "https://registry-1.docker.io",
                       command: Seq[String] = Seq()) extends ContainerImage

case class SavedDockerImage(imageName: String,
                             file: File,
                            compressed: Boolean = false,
                            command: Seq[String] = Seq()) extends ContainerImage

case class PreparedImage(file: File,
                         manifestData: ManifestData,
                         configurationData: ConfigurationData,
                         command: Seq[String] = Seq()) extends ContainerImage

case class BuiltPRootImage(file: File,
                           configurationData: ConfigurationData,
                           command: Seq[String] = Seq()) extends ContainerImage

case class BuiltDockerImage(file: File,
                            imageName: String,
                         //   configurationData: ConfigurationData,
                            command: Seq[String] = Seq()) extends ContainerImage

object Stream {
  def copy(inputStream: InputStream, outputStream: OutputStream) = {
    val DefaultBufferSize = 16 * 1024
    val buffer = new Array[Byte](DefaultBufferSize)
    Iterator.continually(inputStream.read(buffer)).takeWhile(_ != -1).foreach {
      outputStream.write(buffer, 0, _)
    }
  }
}
//






// RAW IMPORTS

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
        }
        else {
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
    def client(preventGetHeaderForward: Boolean = false)(implicit networkService: NetworkService) =
      networkService.httpProxy match {
      case Some(httpHost: HttpHost) ⇒ builder(preventGetHeaderForward = preventGetHeaderForward).setProxy(httpHost).build()
      case _                        ⇒ builder(preventGetHeaderForward = preventGetHeaderForward).build()
    }

    def execute[T](get: HttpGet, checkError: Boolean = true, preventGetHeaderForward: Boolean = false)(f: HttpResponse ⇒ T)(implicit networkService: NetworkService) = {
      val response = client(preventGetHeaderForward = preventGetHeaderForward)(networkService).execute(get)

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
  case class Manifest(value: ImageManifestV2Schema1, image: DockerImage)

  object Token {

    case class AuthenticationRequest(scheme: String, realm: String, service: String, scope: String)
    case class Token(scheme: String, token: String)

    def withToken(url: String, timeout: Time)(implicit networkservice: NetworkService) = {
      val get = new HttpGet(url)
      get.setConfig(RequestConfig.custom().setConnectTimeout(timeout.millis.toInt).setConnectionRequestTimeout(timeout.millis.toInt).build())

      val authenticationRequest = authentication(get)

      val t = token(authenticationRequest.get) match {
        case Left(l)  ⇒ throw new RuntimeException(s"Failed to obtain authentication token: $l")
        case Right(r) ⇒ r
      }

      val request = new HttpGet(url)
      request.addHeader("Authorization", s"${t.scheme} ${t.token}")
      request.setConfig(RequestConfig.custom().setConnectTimeout(timeout.millis.toInt).setConnectionRequestTimeout(timeout.millis.toInt).build())
      request
    }

    def authentication(get: HttpGet)(implicit networkservice: NetworkService) = execute(get, checkError = false) { response ⇒
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
      }
    }

    def token(authenticationRequest: AuthenticationRequest)(implicit networkservice: NetworkService): Either[Err, Token] = {
      val tokenRequest = s"${authenticationRequest.realm}?service=${authenticationRequest.service}&scope=${authenticationRequest.scope}"

      val get = new HttpGet(tokenRequest)
      execute(get) { response ⇒
        // @Romain could be done with optics at the cost of an extra dependency ;)
        val tokenRes = for {
          parsed ← parse(content(response))
          token ← parsed.hcursor.get[String]("token")
        } yield Token(authenticationRequest.scheme, token)

        tokenRes.leftMap(l ⇒ Err(l.getMessage))
      }
    }

  }

  def baseURL(image: DockerImage): String = {
    val path = if (image.imageName.contains("/")) image.imageName else s"library/${image.imageName}"
    s"${image.registry}/v2/$path"
  }

  def downloadManifest(image: DockerImage, timeout: Time)(implicit networkService: NetworkService): String = {
    val url = s"${baseURL(image)}/manifests/${image.tag}"
    val httpResponse = client(preventGetHeaderForward = true).execute(Token.withToken(url, timeout))

    if (httpResponse.getStatusLine.getStatusCode >= 300)
      throw new UserBadDataError(s"Docker registry responded with $httpResponse to query of image $image")
    content(httpResponse)
  }

  def manifest(image: DockerImage, manifestContent: String): Either[Err, Manifest] = {
    val manifestsE = decode[ImageManifestV2Schema1](manifestContent)
    val manifest = for {
      manifest ← manifestsE
    } yield Manifest(manifest, image)
    manifest.leftMap(err ⇒ Err(err.getMessage))
  }

  def layers(manifest: ImageManifestV2Schema1): Seq[Layer] = for {
    fsLayers ← manifest.fsLayers.toSeq
    fsLayer ← fsLayers
  } yield Layer(fsLayer.blobSum)

  def blob(image: DockerImage, layer: Layer, file: BFile, timeout: Time)(implicit networkservice: NetworkService): Unit = {
    val url = s"""${baseURL(image)}/blobs/${layer.digest}"""
    execute(Token.withToken(url, timeout), preventGetHeaderForward = true) { response ⇒
      val os = file.newOutputStream
      try Stream.copy(response.getEntity.getContent, os)
      finally os.close()
    }
  }

  /*
    * Download layer file from image if not already present in layers destination directory
    *
    * @param dockerImage DockerImage which layer to download
    * @param layer layer to download
    * @param layersDirectory Layers destination directory
    * @param layerFile Destination file for the layer within destination directory
    * @param timeout Download timeout
    * @param newFile OM temporary file creation service
    */
}

