package container

import java.util.concurrent.ExecutorService

import container.ImageDownloader.Executor
import org.apache.http.HttpHost
import container.OCI.{ManifestData, _}
import container.Status.note
import Registry._

import scala.sys.process._
import io.circe._
import io.circe.parser._
import DockerMetadata._
import better.files.{File => BFile}
import squants.time._
import java.io.File

import scala.concurrent.Future

object ImageDownloader {

  case class ContainerConf(cmd: List[String])

  case class Config(id: String,
                    parent: String,
                    created: String,
                    containerConfig: ContainerConf)

  /*
json = {"id":"8a71d4786d3d25edf9acf9e6783a1e3db45dd267be3033c3d1970c0552f7a95f","parent":"e8d1c98545ca7e1b15f039c134c6cbdfe9c470563e1c602f7dbb884727b94827","created":"2019-02-06T03:30:02.095682729Z","container_config":{"Cmd":["/bin/sh -c #(nop)  CMD [\"bash\"]"]},"throwaway":true}
*/
  case class ImageNotFound(image: DockerImage) extends Exception

  def downloadImageWithDocker(dockerImage: DockerImage): SavedDockerImage = {
    note("- download Image from DockerHub")
    val name = dockerImage.imageName + ":" + dockerImage.tag
    val fileName = dockerImage.imageName + ".tar"
    ("docker pull " + name).!!
    note("- save DockerImage as a .tar")
    val file = BFile(fileName).createFileIfNotExists()
    ("docker save -o " + fileName + " " + name).!!
    SavedDockerImage(dockerImage.imageName, file.toJava, command = dockerImage.command)
  }

  def getManifestAsString(layersHash: List[String], name: String, tag: String): String = {
    val config = "[{\"Config\":\"config.json\","
    val repotag = "\"RepoTags\":[\"" + name + ":" + tag + "\"],"
    val layers = "\"Layers\":["
    var manifest = config + repotag + layers
    val last = layersHash.reverse.last
    for (hash <- layersHash.reverse.init)
      manifest = manifest + "\"" + hash + "/layer.tar\","
    manifest + "\"" + last + "/layer.tar\"]}]"
  }
/*
  def getConfigAsString(manifest: Manifest, name: String): String = {
    var config = "{"
    val last = manifest.value.history.get.last.v1Compatibility.substring(1, manifest.value.history.get.last.v1Compatibility.length)
    for (x <- manifest.value.history.get.init) {
      config = config + x.v1Compatibility.substring(1, x.v1Compatibility.length - 1) + ",\n"
    }
    config + last
  }
  */
   def getConfigAsString(manifest: Manifest/*, layersHash: List[String]*/) = {
      val x = imageJSONEncoder(v1HistoryToImageJson(manifest)).toString()
     println(x)
     x
   }

  def writeConfigFiles(path: String, manifest: String, config: String): Unit = {
    note(" -- writing file config.json")
    BFile(path + "/config.json").appendLine(config)
    note(" -- writing file manifest.json")
    BFile(path + "/manifest.json").appendLine(manifest)
  }

  def writeManifestFile(path: String, manifest: String): Unit = {
    note(" -- writing file manifest.json")
    BFile(path + "/manifest.json").appendLine(manifest)
  }

/*
  def downloadImageForPRoot(dockerImage: DockerImage, writeConfigFile: Boolean = true): PreparedImage = {
    note(" - creating image directory")
    val dir = BFile(dockerImage.imageName).createDirectoryIfNotExists()

    // SEE WHAT TO DO WITH THIS--------
    val opt: Option[HttpHost] = None //
    val net = new NetworkService(opt) //
    //---------------------------------


    manifest(dockerImage, downloadManifest(dockerImage, Seconds(10))(net)) match {
      case Right(value) => {
        // 1. First get the configData
        note(" - collecting configData")
        val configString = getConfigAsString(value)//, dockerImage.imageName)
        val configurationData: ConfigurationData = harvestConfigData(configString)

        // 2. Get the manifestData
        note(" - collecting manifestData")
        val layersHash = value.value.fsLayers.get.map(_.blobSum)
        //.distinct
        val manifestString = getManifestAsString(layersHash, dockerImage.imageName, dockerImage.tag)
        val manifestData: ManifestData = harvestManifestData(manifestString)

        // 3. Download the layers
        for (hash <- layersHash) {
          // if (hash != "sha256:a3ed95caeb02ffe68cdd9fd84406680ae93d633cb16422d00e8a7c22955b46d4") {
          blob(dockerImage, Layer(hash), BFile(dir.pathAsString + "/" + hash + ".tar.gz"), Seconds(10))(net)
          note(" - downloading image's layer : " + hash + ".tar.gz")
          //}
        }
        if (writeConfigFile)
          writeConfigFiles(dir.pathAsString, manifestString, configString)

        PreparedImage(dir.toJava, manifestData, configurationData, dockerImage.command)
      }
      case _ => throw ImageNotFound(dockerImage)
    }
  }
*/

  import scala.concurrent.Future
  import scala.concurrent.ExecutionContext.Implicits.global

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
    note(" - creating image directory")
    val imagePath = path.getAbsolutePath + "/" + dockerImage.imageName
    val dir = BFile(imagePath).createDirectoryIfNotExists()

    // SEE WHAT TO DO WITH THIS-------//
    val opt: Option[HttpHost] = None //
    val net = new NetworkService(opt) //
    //--------------------------------//

    manifest(dockerImage, downloadManifest(dockerImage, timeout)(net)) match {
      case Right(value) => {
        // 1. First get the configData
        note(" - collecting configData")
        val configString = getConfigAsString(value)//, dockerImage.imageName)
        val configJson = parse(configString).getOrElse(Json.Null)

        // 2. Get the manifestData
        note(" - collecting manifestData")
        var conf = value.value.history.get
        val layersIDS = {
          val raw = conf.map(_.v1Compatibility)
          for (x <- raw) yield {
            val parsed = parse(x).getOrElse(Json.Null)
            val cursor: HCursor = parsed.hcursor
            cursor.get[String]("id") match {
              case Right(id) => id
              case Left(error) => throw error
            }
          }
        }
        val layersHash = value.value.fsLayers.get.map(_.blobSum)
        val manifestString = getManifestAsString(layersIDS, dockerImage.imageName, dockerImage.tag)
  //      println("ImageDownloader -> harvestManifestData:\n" + manifestString)
        val manifestData: ManifestData = harvestManifestData(manifestString)

        // 3. Write manifest and config files
        note(" - writing manifest file")
        writeConfigFiles(dir.pathAsString, manifestString, configString)

        // 4. Download the layers
        note(" - downloading layers")
        for ((hash, id) <- layersHash zip layersIDS) {
            note(" -- creating layer's dir")
            val layerDir = BFile(dir.pathAsString + "/" + id).createDirectoryIfNotExists()

            note(" -- writing VERSION file")
            BFile(layerDir.pathAsString + "/VERSION").appendLine("1.0")

            note(" -- downloading layer.tar into the " + id + " dir")
            blob(dockerImage, Layer(hash), BFile(layerDir.pathAsString + "/" + "layer.tar"), Seconds(10))(net)

            note(" -- downloading json config file into the " + id + " dir")
            if (!BFile(layerDir.pathAsString + "/json").exists && conf.nonEmpty) {
              BFile(layerDir.pathAsString + "/json").appendLine(conf.head.v1Compatibility)
              conf = conf.tail
            }
          }
          SavedDockerImage(dockerImage.imageName, dir.toJava)
        }
          case _ => throw ImageNotFound(dockerImage)
        }
      }

  }


// TAR :  tar -c hello-seattle > hello-seattle.tar.gz