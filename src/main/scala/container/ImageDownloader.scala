package container

import java.io.File
import java.util.UUID
import java.util.concurrent.ExecutorService

import better.files.{File => BFile}
import container.DockerMetadata._
import container.Registry._
import io.circe._
import io.circe.parser._
import org.apache.http.HttpHost
import squants.time._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.sys.process._

object ImageDownloader {

  case class ContainerConf(cmd: List[String])
  case class Config(id: String,
                    parent: String,
                    created: String,
                    containerConfig: ContainerConf)
  case class ImageNotFound(image: DockerImage) extends Exception

  def downloadImageWithDocker(dockerImage: DockerImage): SavedDockerImage = {
    val name = dockerImage.imageName + ":" + dockerImage.tag
    val fileName = dockerImage.imageName + ".tar"
    ("docker pull " + name).!!
    val file = BFile(fileName).createFileIfNotExists()
    ("docker save -o " + fileName + " " + name).!!
    SavedDockerImage(dockerImage.imageName, file.toJava, command = dockerImage.command)
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

   def getConfigAsString(manifest: Manifest, layersHash: Map[String, Option[String]]) = {
      val x = imageJSONEncoder(v1HistoryToImageJson(manifest, layersHash)).toString()
     x
   }

  def writeManifestFile(path: String, manifest: String): Unit = {
    BFile(path + "/manifest.json").appendLine(manifest)
  }

  object Executor {
    def sequential = new Executor {
      override def apply[T](f: => T): Future[T] = Future.fromTry {
        util.Try { f: T }
      }
    }

     def parallel(implicit executorService: ExecutorService) = new Executor {
      override def apply[T](f: => T): Future[T] = Future(f)
    }
  }

  sealed trait Executor {
    def apply[T](f: => T): Future[T]
  }

//  () => T => Future[T]

  ///////////////////
  def downloadContainerImage(dockerImage: DockerImage, path: File, timeout: Time): SavedDockerImage = {
    val imagePath = path.getAbsolutePath + "/" + dockerImage.imageName
    val dir = BFile(imagePath).createDirectoryIfNotExists()

    // SEE WHAT TO DO WITH THIS-------//
    val opt: Option[HttpHost] = None //
    val net = new NetworkService(opt) //
    //--------------------------------//

    manifest(dockerImage, downloadManifest(dockerImage, timeout)(net)) match {
      case Right(manifestValue) =>
          var conf = manifestValue.value.history.get

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
          val layersHashMap = collection.mutable.HashMap[String, Option[String]]()

          for {
            (hash, (id, ignore)) <- layersHash zip layersIDS
          } if(!ignore) {
            val dirName = UUID.randomUUID().toString
            val layerDir = dir / dirName
            layerDir.createDirectories()

            BFile(layerDir.pathAsString + "/VERSION").appendLine("1.0")

            blob(dockerImage, Layer(hash), BFile(layerDir.pathAsString + "/" + "layer.tar"), timeout)(net)
            val layerHash = Hash.sha256(BFile(layerDir.pathAsString + "/" + "layer.tar").toJava)

            layersHashMap.put(hash, Some(layerHash))

            if (!BFile(layerDir.pathAsString + "/json").exists && conf.nonEmpty) {
              BFile(layerDir.pathAsString + "/json").appendLine(conf.head.v1Compatibility)
              conf = conf.tail
            }

            layerDir moveTo (dir / layerHash)
          } else layersHashMap.put(hash, None)

          val configString = getConfigAsString(manifestValue, layersHashMap.toMap)
          val configName = Hash.sha256(configString) + ".json"
          BFile(dir.pathAsString + s"/$configName").appendLine(configString)

          val manifestString = getManifestAsString(layersHash.flatMap(l => layersHashMap(l)), dockerImage.imageName, dockerImage.tag, configName)
          BFile(dir.pathAsString + "/manifest.json").appendLine(manifestString)


          SavedDockerImage(dockerImage.imageName, dir.toJava)
        case _ => throw ImageNotFound(dockerImage)
      }
    }
  }