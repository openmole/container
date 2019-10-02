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

import java.io.File
import java.util.UUID
import java.util.concurrent.{ExecutorService, Executors, ThreadFactory}

import better.files.{File => BFile}
import container.DockerMetadata._
import container.Registry._
import io.circe._
import io.circe.parser._
import org.apache.http.HttpHost
import squants.time._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.sys.process._

object ImageDownloader {

  case class ContainerConf(cmd: List[String])
  case class Config(
    id: String,
    parent: String,
    created: String,
    containerConfig: ContainerConf)
  case class ImageNotFound(image: RegistryImage) extends Exception

  def downloadImageWithDocker(dockerImage: RegistryImage): SavedImage = {
    val name = dockerImage.imageName + ":" + dockerImage.tag
    val fileName = dockerImage.imageName + ".tar"
    ("docker pull " + name).!!
    val file = BFile(fileName).createFileIfNotExists()
    ("docker save -o " + fileName + " " + name).!!
    SavedImage(dockerImage.imageName, file.toJava, command = dockerImage.command)
  }

  def getManifestAsString(layersHash: List[String], name: String, tag: String, configName: String): String = {
    val config = "[{\"Config\":\"" + configName + "\","
    val repotag = "\"RepoTags\":[\"" + name + ":" + tag + "\"],"
    val layers = "\"Layers\":["
    var manifest = config + repotag + layers
    val last = layersHash.reverse.last
    for (hash <- layersHash.reverse.init) manifest = manifest + "\"" + hash + "/layer.tar\","
    manifest + "\"" + last + "/layer.tar\"]}]"
  }

   def getConfigAsString(manifest: Manifest, layersHash: Map[String, Option[String]]) =
     imageJSONEncoder(v1HistoryToImageJson(manifest, layersHash)).toString()


  def writeManifestFile(path: String, manifest: String): Unit = {
    BFile(path + "/manifest.json").appendLine(manifest)
  }

  object Executor {
    def sequential = new Executor {
      override def apply[T](f: => T): Future[T] = Future.fromTry {
        util.Try { f: T }
      }
    }

    private val daemonFactory = new ThreadFactory {
      override def newThread(r: Runnable): Thread = {
        val t = new Thread(r)
        t.setDaemon(true)
        t
      }
    }

    def parallel(implicit executorService: ExecutorService = Executors.newFixedThreadPool(10, daemonFactory)) = new Executor {
      override def apply[T](f: => T): Future[T] =
        Future { f } (ExecutionContext.fromExecutorService(executorService))
    }
  }

  sealed trait Executor {
    def apply[T](f: => T): Future[T]
  }

  def downloadContainerImage(
    dockerImage: RegistryImage,
    localRepository: File,
    timeout: Time,
    executor: Executor = Executor.sequential,
    proxy: Option[HttpHost] = None): SavedImage = {
    import better.files._

    val tmpDirectory = localRepository.toScala / ".tmp"
    val imageDirectory = localRepository.toScala /  dockerImage.imageName
    val idsDirectory = imageDirectory / "id"

    tmpDirectory.createDirectoryIfNotExists()
    imageDirectory.createDirectoryIfNotExists()
    idsDirectory.createDirectoryIfNotExists()

    manifest(dockerImage, downloadManifest(dockerImage, timeout, proxy = proxy)) match {
      case Right(manifestValue) =>
        val conf = manifestValue.value.history.get

        val layersIDS = {
          val raw = conf.map(_.v1Compatibility)

          raw.map { x =>
            val parsed = parse(x).getOrElse(Json.Null)
            val cursor: HCursor = parsed.hcursor
            val ignore =
              cursor.get[Boolean]("throwaway") match {
                case Right(value) => value
                case Left(error) => false
              }

              cursor.get[String]("id") match {
                case Right(id) => id -> ignore
                case Left(error) => throw error
              }
          }
        }

        val layersHash = manifestValue.value.fsLayers.get.map(_.blobSum)
        val infiniteConfig: Iterator[Option[String]] = conf.map(c => Some(c.v1Compatibility)).toIterator ++ Iterator.continually(None)

        val layersMap =
          for {
            ((hash, (id, ignore)), v1compat) <- (layersHash.toIterator zip layersIDS.toIterator zip infiniteConfig)
          } yield executor {
            val idFile = idsDirectory / id
            if(!ignore) {
              if(!idFile.exists) {
                val dirName = UUID.randomUUID().toString
                val tmpLayerDir = tmpDirectory / dirName

                tmpLayerDir.createDirectories()

                (tmpLayerDir / "VERSION").appendLine("1.0")

                downloadBlob(dockerImage, Layer(hash), tmpLayerDir / "layer.tar", timeout, proxy = proxy)

                val layerHash = Hash.sha256(tmpLayerDir / "layer.tar" toJava)

                if (!(tmpLayerDir.pathAsString / "json").exists && v1compat.nonEmpty) {
                  (tmpLayerDir / "json").appendLine(v1compat.get)
                }

                lock.withLockInDirectory(idsDirectory.toJava) {
                  if (!idFile.exists) {
                    val layerPath = imageDirectory / layerHash
                    tmpLayerDir moveTo layerPath
                    idFile.createFile
                    idFile write layerHash
                    hash -> Some(layerHash)
                  } else {
                    tmpLayerDir.delete()
                    hash -> Some(idFile.contentAsString)
                  }
                }

              } else hash -> Some(idFile.contentAsString)
            } else hash -> None
          }

        val layersHashMap = Await.result(Future.sequence(layersMap), Duration.Inf).toMap
        val configString = getConfigAsString(manifestValue, layersHashMap)

          // should it be written each time
        val configName = Hash.sha256(configString) + ".json"
        (imageDirectory / configName) write configString

        val manifestString = getManifestAsString(layersHash.flatMap(l => layersHashMap(l)), dockerImage.imageName, dockerImage.tag, configName)
        (imageDirectory / "manifest.json") write manifestString

        SavedImage(dockerImage.imageName, imageDirectory.toJava)
      case _ => throw ImageNotFound(dockerImage)
    }
  }
}