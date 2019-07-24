package container

import java.io.File
import better.files.{File => BFile, _}
import container.Extractor._
import container.OCI._

object ImageBuilder {
  case class FileNotFound(file: File) extends Exception
  case class InvalidImage(file: File) extends Exception
  case class DirectoryFileCollision(file: File) extends Exception
  case class CommandExecutionError(status: Int, stdout: String, stderr: String) extends Exception
  val rootfsName = "rootfs"

  def buildImageForDocker(image: SavedDockerImage, archive: File): BuiltDockerImage = {
    if (image.compressed) BuiltDockerImage(image.file, image.imageName, image.command)
    //val path = image.file.toScala.pathAsString
    //BFile(path + "/manifest.json").delete()
    Tar.archive(image.file, archive)
    BuiltDockerImage(archive, image.imageName, image.command)
  }

  def buildImageForProot(image: SavedDockerImage, workDirectory: File): BuiltPRootImage =
    buildImage(analyseImage(extractImage(image, workDirectory)), workDirectory)

  def extractImage(savedDockerImage: SavedDockerImage, workDirectory: File): SavedDockerImage = {
    checkImageFile(savedDockerImage.file)
    if (!savedDockerImage.file.isDirectory) {
      if(!isAnArchive(savedDockerImage.file.getAbsolutePath)) throw InvalidImage(savedDockerImage.file)
      val directory = workDirectory
      workDirectory.mkdirs()
      Tar.extract(savedDockerImage.file, directory)
      SavedDockerImage(savedDockerImage.imageName, directory, false, savedDockerImage.command)
    } else savedDockerImage
  }
    
    /** Retrieve metadata (layer ids, env variables, volumes, ports, commands)
      * from the manifest and configuration files.
      * Return a PreparedImage with Manifest an Config data
      */
    def analyseImage(savedDockerImage: SavedDockerImage): PreparedImage = {
        checkImageFile(savedDockerImage.file)
        val filePath = savedDockerImage.file.getAbsolutePath + "/"
        val manifestContent = BFile(filePath + "manifest.json").contentAsString
        val manifestData: ManifestData = harvestManifestData(manifestContent.substring(0, manifestContent.length -1))
        val configurationData: ConfigurationData = manifestData.Config match {
          case Some(conf) => {
            val configContent = BFile(filePath + conf).contentAsString
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
    def buildImage(preparedImage: PreparedImage, workDirectory: File): BuiltPRootImage = {
        //checkImageFile(preparedImage.file)
        //val directoryPath = workDirectory.getAbsolutePath + "/"
        val rootfsPath = workDirectory.toScala / rootfsName
        rootfsPath.createDirectoryIfNotExists()

        val layers = preparedImage.manifestData.Layers
        layers.foreach{
          layerName =>
            Tar.archive((preparedImage.file.toScala / layerName).toJava, rootfsPath.toJava)
            removeWhiteouts(rootfsPath.toJava)
        }
        BuiltPRootImage(workDirectory, preparedImage.configurationData, preparedImage.command)
    }

  def checkImageFile(file: File): Unit = if (!file.exists()) throw FileNotFound(file)
}
