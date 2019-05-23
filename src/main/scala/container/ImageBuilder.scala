package container

import java.io.File
import better.files.{File => BFile, _}
import container.Status._
import container.Extractor._
import container.OCI._
import scala.sys.process._

object ImageBuilder {

  case class FileNotFound(file: File) extends Exception
  case class InvalidImage(file: File) extends Exception
  case class DirectoryFileCollision(file: File) extends Exception
  case class CommandExecutionError(status: Int, stdout: String, stderr: String) extends Exception
  val rootfsName = "rootfs"

    def buildImageForDocker(image: SavedDockerImage): BuiltDockerImage = {
      if (image.compressed) BuiltDockerImage(image.file, image.imageName, image.command)
      val path = image.file.toScala.pathAsString
      BFile(path + "/manifest.json").delete()
      compressFile(path, image.imageName)
      BuiltDockerImage(BFile(image.imageName + ".tar").toJava, image.imageName, image.command)
    }

    def buildImageForProot(image: SavedDockerImage): BuiltPRootImage =
      buildImage(analyseImage(extractImage(image)))

    def extractImage(savedDockerImage: SavedDockerImage): SavedDockerImage = {
      checkImageFile(savedDockerImage.file)
      if (!savedDockerImage.file.isDirectory) {
        val (path, archiveName) = getPathAndFileName(savedDockerImage.file.getAbsolutePath)
        if(!isAnArchive(archiveName)) throw InvalidImage(savedDockerImage.file)
          val directoryPath = getDirectoryPath(path, archiveName)
          if(BFile(directoryPath).exists && !BFile(directoryPath).isDirectory)
              throw DirectoryFileCollision(BFile(directoryPath).toJava)
          val directory = BFile(directoryPath).createDirectoryIfNotExists()
          extractArchive(path + archiveName, directoryPath)
          SavedDockerImage(savedDockerImage.imageName, directory.toJava, false, savedDockerImage.command)
      }
      else savedDockerImage
    }
    
    /** Retrieve metadata (layer ids, env variables, volumes, ports, commands)
      * from the manifest and configuration files.
      * Return a PreparedImage with Manifest an Config data
      */
    def analyseImage(savedDockerImage: SavedDockerImage): PreparedImage = {
        checkImageFile(savedDockerImage.file)

        val filePath = savedDockerImage.file.getAbsolutePath + "/"

        note("-- extracting manifest data")
        val manifestContent = BFile(filePath + "manifest.json").contentAsString
//        println("\nImageBuilder -> harvestManifestData:\n" + manifestContent.substring(0, manifestContent.length -1))
        val manifestData: ManifestData = harvestManifestData(manifestContent.substring(0, manifestContent.length -1))


        note("-- extracting configuration data")
        val configurationData: ConfigurationData = manifestData.Config match {
          case Some(conf) => {
            val configContent = BFile(filePath + conf).contentAsString
 //           println("\nImageBuilder -> harvestConfigData:\n" + configContent) // .substring(0, manifestContent.length -1)
            harvestConfigData(configContent)
          }
          case _ => ConfigurationData(None,None,None,None,None, None, None)
        }
        PreparedImage(savedDockerImage.file, manifestData, configurationData, savedDockerImage.command)
    }
    
    /** Merge the layers by extracting them in order in a same directory.
      * Also, delete the whiteout files.
      * Return a BuiltImage
      */
    def buildImage(preparedImage: PreparedImage): BuiltPRootImage = {
        checkImageFile(preparedImage.file)

        val directoryPath = preparedImage.file.getAbsolutePath + "/"
        val rootfsPath = directoryPath + rootfsName + "/"
        BFile(rootfsPath).createDirectoryIfNotExists()
        val layers = preparedImage.manifestData.Layers
        layers.foreach{
            layerName => {
                note("-- extracting layer " + layerName)
                extractArchive(directoryPath + layerName, rootfsPath)
                removeWhiteouts(rootfsPath)
            }
        }
        BuiltPRootImage(preparedImage.file, preparedImage.configurationData, preparedImage.command)
    }

  def checkImageFile(file: File): Unit = {
    note(s"checkImage: \nfile: $file")
    if (!file.exists()) throw FileNotFound(file)
  }
}
