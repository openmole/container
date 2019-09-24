/*
 * Copyright (C) 2016 Vincent Hage
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
import better.files.{File => BFile, _}
import container.Extractor._
import container.OCI._

object ImageBuilder {
  case class FileNotFound(file: File) extends Exception
  case class InvalidImage(file: File) extends Exception
  case class DirectoryFileCollision(file: File) extends Exception
  case class CommandExecutionError(status: Int, stdout: String, stderr: String) extends Exception
  val rootfsName = "rootfs"


  def extractImage(savedDockerImage: SavedImage, workDirectory: File): SavedImage = {
    checkImageFile(savedDockerImage.file)
    if (!savedDockerImage.file.isDirectory) {
      if(!isAnArchive(savedDockerImage.file.getAbsolutePath)) throw InvalidImage(savedDockerImage.file)
      val directory = workDirectory
      workDirectory.mkdirs()
      Tar.extract(savedDockerImage.file, directory)
      SavedImage(savedDockerImage.imageName, directory, false, savedDockerImage.command)
    } else savedDockerImage
  }
    
    /** Retrieve metadata (layer ids, env variables, volumes, ports, commands)
      * from the manifest and configuration files.
      * Return a PreparedImage with Manifest an Config data
      */
    def prepareImage(savedDockerImage: SavedImage): PreparedImage = {
        checkImageFile(savedDockerImage.file)
        val filePath = savedDockerImage.file.getAbsolutePath + "/"
        val manifestContent = BFile(filePath + "manifest.json").contentAsString
        val manifestData: ManifestData = harvestManifestData(manifestContent)
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
    def buildImage(preparedImage: PreparedImage, rootfs: File): Unit = {
      //checkImageFile(preparedImage.file)
      //val directoryPath = workDirectory.getAbsolutePath + "/"
      rootfs.toScala.createDirectoryIfNotExists()


      extractLayers(preparedImage, rootfs)
//      val layers = preparedImage.manifestData.Layers
//
//      layers.foreach{
//        layerName =>
//          Tar.extract((preparedImage.file.toScala / layerName).toJava, rootfsPath.toJava)
//          removeWhiteouts(rootfsPath.toJava)
//      }
    }

  def extractLayers(preparedImage: PreparedImage, destination: File) = {
    destination.toScala.createDirectoryIfNotExists()

    val layers = preparedImage.manifestData.Layers

    layers.foreach{
      layerName =>
        Tar.extract((preparedImage.file.toScala / layerName).toJava, destination)
        removeWhiteouts(destination)
    }
  }

  def checkImageFile(file: File): Unit = if (!file.exists()) throw FileNotFound(file)
}
