/*
 * Copyright (C) 2019 Pierre Peigne
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package container

import java.util.UUID
import java.util.concurrent.{ ExecutorService, Executors, ThreadFactory }

import better.files.{ File => BFile }
import com.sun.net.httpserver.Authenticator.Success
import container.DockerMetadata._
import container.Registry._
import container.tool.{ Retry, lock }
import io.circe._
import io.circe.parser._
import org.apache.http.HttpHost
import squants.time._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.sys.process._

object ImageDownloader {

  object HttpProxy:
    def toHost(httpProxy: HttpProxy) = HttpHost.create(httpProxy.uri)

  case class HttpProxy(uri: String)

  case class ContainerConf(cmd: List[String])
  case class Config(
    id: String,
    parent: String,
    created: String,
    containerConfig: ContainerConf)

  case class ImageNotFound(image: RegistryImage, e: Option[Throwable] = None) extends Exception(e.orNull)

  //  def downloadImageWithDocker(dockerImage: RegistryImage): SavedImage = {
  //    val name = dockerImage.imageName + ":" + dockerImage.tag
  //    val fileName = dockerImage.imageName + ".tar"
  //    ("docker pull " + name).!!
  //    val file = BFile(fileName).createFileIfNotExists()
  //    ("docker save -o " + fileName + " " + name).!!
  //    SavedImage(file.toJava) //, command = dockerImage.command)
  //  }

//  def getConfigAsString(manifest: ImageManifestV2Schema1, layersHash: Map[String, Option[String]]) =
//    imageJSONEncoder(v1HistoryToImageJson(manifest, layersHash)).toString()

  def writeManifestFile(path: String, manifest: String): Unit =
    BFile(path + "/manifest.json").appendLine(manifest)

  object Executor:
    def sequential = new Executor:
      override def apply[T](f: => T): Future[T] = Future.fromTry:
        util.Try { f: T }

    private val daemonFactory = new ThreadFactory:
      override def newThread(r: Runnable): Thread =
        val t = new Thread(r)
        t.setDaemon(true)
        t

    def parallel(implicit executorService: ExecutorService = Executors.newFixedThreadPool(10, daemonFactory)) = new Executor:
      override def apply[T](f: => T): Future[T] =
        Future { f }(ExecutionContext.fromExecutorService(executorService))


  sealed trait Executor:
    def apply[T](f: => T): Future[T]

  def imageDirectory(localRepository: java.io.File, image: RegistryImage) =
    import better.files._
    (localRepository.toScala / image.name / image.tag).toJava

  def downloadContainerImage(
    dockerImage: RegistryImage,
    localRepository: java.io.File,
    timeout: Time,
    retry: Option[Int] = None,
    executor: Executor = Executor.sequential,
    proxy: Option[HttpProxy] = None): SavedImage = 
    import better.files._

    val retryCount = retry.getOrElse(0)

    val manifestString = Retry.retry(retryCount)(downloadManifest(dockerImage, timeout, proxy = proxy.map(HttpProxy.toHost)))
    val decodedManifest = decodeManifest(manifestString)

    decodedManifest match
      case util.Success(manifestValue) =>
        val tmpDirectory = localRepository.toScala / ".tmp"
        val imageDirectoryValue = imageDirectory(localRepository, dockerImage).toScala
        val existingIndexDirectory = imageDirectoryValue / "hash"

        tmpDirectory.createDirectoryIfNotExists()
        imageDirectoryValue.createDirectoryIfNotExists()
        existingIndexDirectory.createDirectoryIfNotExists()


        def fromManifestV2(manifestValue: ImageManifestV2Schema2) =
          val layers = manifestValue.layers.map(_.digest)

          val configString =
            val url = s"""${baseURL(dockerImage)}/blobs/${manifestValue.config.digest}"""
            val headers = Seq("Accept" -> manifestValue.config.mediaType)
            Retry.retry(retryCount)(download(url, timeout, proxy = proxy.map(HttpProxy.toHost), headers = headers))

          (layers.reverse, configString)

        val (layerHashes, configString) =
          manifestValue match
            case imv: ImageManifestV2Schema2List =>
              import io.circe.generic.auto.*
              val manifests = imv.manifests
              
              val mid = manifests.find(m => m.platform.architecture == "amd64" && m.platform.os == "linux").getOrElse(throw RuntimeException("No image found for amd64 on linux, manifest is " + manifests))
              val query = s"${baseURL(dockerImage)}/manifests/${mid.digest}"
              val headers = Seq("Accept" -> mid.mediaType)
              val manifestString = Retry.retry(retryCount)(download(query, timeout, proxy = proxy.map(HttpProxy.toHost), headers = headers))
              val manifestValue = decode[ImageManifestV2Schema2](manifestString).toTry.get
              fromManifestV2(manifestValue)
            case manifestValue: ImageManifestV2Schema2 => fromManifestV2(manifestValue)
            case manifestValue: ImageManifestV2Schema1 =>
              val conf = manifestValue.history.get
              val raw = conf.map(_.v1Compatibility)

              val ignores =
                raw.map: x =>
                  import io.circe.generic.auto.*

                  val v1Compat = decode[V1History.V1Compatibility](x).toTry.get
                  val ignore = v1Compat.throwaway.getOrElse(false)
                  val id = v1Compat.id
                  ignore

              val layersHash =
                val hashes = manifestValue.fsLayers.get.map(_.blobSum)
                (hashes zip ignores).filter(!_._2).map(_._1)

              (layersHash, conf.head.v1Compatibility)


        val layersMap: Seq[Future[(String, String)]] =
          for
            hash <- layerHashes
          yield executor:
            val idFile = existingIndexDirectory / hash
            if !idFile.exists
            then
              val dirName = UUID.randomUUID().toString
              val tmpLayerDir = tmpDirectory / dirName

              tmpLayerDir.createDirectories()

              (tmpLayerDir / "VERSION").appendLine("1.0")

              Retry.retry(retryCount)(downloadBlob(dockerImage, Layer(hash), tmpLayerDir / "layer.tar", timeout, proxy = proxy.map(HttpProxy.toHost)))

              val layerHash = Hash.sha256(tmpLayerDir / "layer.tar" toJava)

              lock.withLockInDirectory(existingIndexDirectory.toJava):
                if !idFile.exists
                then
                  val layerPath: File = imageDirectoryValue / layerHash
                  //tmpLayerDir.moveTo(layerPath)
                  if !layerPath.exists
                  then java.nio.file.Files.move(tmpLayerDir.path, layerPath.path, File.CopyOptions(overwrite = false): _*)
                  else tmpLayerDir.delete()

                  idFile.createFile()
                  idFile write layerHash
                  (hash, layerHash)
                else
                  tmpLayerDir.delete()
                  (hash, idFile.contentAsString)

            else (hash, idFile.contentAsString)

        val layersHashMap = Await.result(Future.sequence(layersMap), Duration.Inf).toMap

        // should it be written each time
        val configName = Hash.sha256(configString) + ".json"
        (imageDirectoryValue / configName) write configString

        val layerFiles =
          layerHashes.map(l => layersHashMap(l)).reverse.map: l =>
          //hashesAndIgnore.map(_._1).flatMap(l => layersHashMap(l)).map: l =>
            l + "/layer.tar"

        val toolManifest =
          List(
            TopLevelImageManifest(
              Config = configName,
              Layers = layerFiles,
              RepoTags = Seq(s"${dockerImage.name}:${dockerImage.tag}")
            )
          )

        val manifestString =
          import io.circe.syntax.*
          import io.circe.generic.auto.*
          toolManifest.asJson.spaces2

        (imageDirectoryValue / "manifest.json") write manifestString

        SavedImage(imageDirectoryValue.toJava)
      case util.Failure(e) => throw ImageNotFound(dockerImage, Some(e))

}