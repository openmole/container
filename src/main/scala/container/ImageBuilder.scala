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

import better.files.{ File => BFile, _ }
import container.Extractor._
import container.OCI._

object ImageBuilder {
  case class FileNotFound(file: File) extends Exception
  case class InvalidImage(file: File) extends Exception
  case class DirectoryFileCollision(file: File) extends Exception
  case class CommandExecutionError(status: Int, stdout: String, stderr: String) extends Exception

  def extractImage(file: File, extractDirectory: File, compressed: Boolean = false): SavedImage = {
    if (!isAnArchive(file.getAbsolutePath)) throw InvalidImage(file)
    extractDirectory.mkdirs()
    Tar.extract(file, extractDirectory, compressed = compressed)
    SavedImage(extractDirectory)
  }

  def flattenImage(image: SavedImage, workDirectory: java.io.File): FlatImage = {
    //    case class PreparedImage(
    //      file: File,
    //      manifestData: ManifestData,
    //      configurationData: ConfigurationData,
    //      command: Seq[String] = Seq())

    //    /**
    //     * Retrieve metadata (layer ids, env variables, volumes, ports, commands)
    //     * from the manifest and configuration files.
    //     * Return a PreparedImage with Manifest an Config data
    //     */
    //    def prepareImage(savedDockerImage: SavedImage): PreparedImage = {
    //      //checkImageFile(savedDockerImage.file)
    //      val filePath = savedDockerImage.file.getAbsolutePath + "/"
    //      val manifestContent = BFile(filePath + "manifest.json").contentAsString
    //      val manifestData: ManifestData = harvestManifestData(manifestContent)
    //      val configurationData: ConfigurationData = manifestData.Config match {
    //        case Some(conf) =>
    //          val configContent = BFile(filePath + conf).contentAsString
    //          harvestConfigData(configContent)
    //        case _ => ConfigurationData(None, None, None, None, None, None, None)
    //      }
    //      PreparedImage(savedDockerImage.file, manifestData, configurationData, savedDockerImage.command)
    //    }

    def extractLayers(savedImage: SavedImage, layers: Vector[String], destination: File) = {
      destination.toScala.createDirectoryIfNotExists()

      layers.foreach {
        layerName =>
          Tar.extract((savedImage.file.toScala / layerName).toJava, destination)
          removeWhiteouts(destination)
      }
    }

    val manifest = Registry.decodeTopLevelManifest((image.file.toScala / "manifest.json").contentAsString).get
    val config = Registry.decodeConfig(image.file.toScala / manifest.Config contentAsString).get

    val rootfs = workDirectory.toScala / FlatImage.rootfsName
    extractLayers(image, manifest.Layers, rootfs.toJava)

    FlatImage(
      file = workDirectory,
      workDirectory = Registry.Config.workDirectory(config),
      env = Registry.Config.env(config))
  }

  def checkImageFile(file: File): Unit = if (!file.exists()) throw FileNotFound(file)
}
