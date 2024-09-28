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
import container.tool.Tar

object ImageBuilder {
  case class FileNotFound(file: File) extends Exception
  case class InvalidImage(file: File) extends Exception
  case class DirectoryFileCollision(file: File) extends Exception
  case class CommandExecutionError(status: Int, stdout: String, stderr: String) extends Exception

  def extractImage(file: File, extractDirectory: File, compressed: Boolean = false): SavedImage =
    if (!isAnArchive(file.getAbsolutePath)) throw InvalidImage(file)
    extractDirectory.mkdirs()
    Tar.extract(file, extractDirectory, compressed = compressed)
    SavedImage(extractDirectory)

  def flattenImage(image: SavedImage, workDirectory: java.io.File): FlatImage =
    def extractLayers(savedImage: SavedImage, layers: Seq[String], destination: File) =
      destination.toScala.createDirectoryIfNotExists()

      layers.zipWithIndex.foreach: (layerName, i) =>
        //println(i + "  " + layerName)
        Tar.extract((savedImage.file.toScala / layerName).toJava, destination, filter = Some(e => OCI.WhiteoutUtils.isWhiteout(java.nio.file.Paths.get(e.getName))))
        removeWhiteouts(destination)
        Tar.extract((savedImage.file.toScala / layerName).toJava, destination, filter = Some(e => !OCI.WhiteoutUtils.isWhiteout(java.nio.file.Paths.get(e.getName))))

    val manifest = Registry.decodeTopLevelManifest((image.file.toScala / "manifest.json").contentAsString).get
    val config = Registry.decodeConfig(image.file.toScala / manifest.Config contentAsString).get

    //println(image.file.toScala / manifest.Config contentAsString)

    val rootfs = workDirectory.toScala / FlatImage.rootfsName
    extractLayers(image, manifest.Layers, rootfs.toJava)

    FlatImage(
      file = workDirectory,
      workDirectory = Registry.Config.workDirectory(config),
      env = Registry.Config.env(config),
      layers = manifest.Layers)

  def duplicateFlatImage(flatImage: FlatImage, directory: java.io.File) =
    FlatImage(
      file = flatImage.file.toScala.copyTo(directory.toScala)(BFile.CopyOptions(overwrite = true) ++ BFile.LinkOptions.noFollow).toJava,
      workDirectory = flatImage.workDirectory,
      env = flatImage.env,
      layers = flatImage.layers,
      command = flatImage.command)

  def checkImageFile(file: File): Unit = if (!file.exists()) throw FileNotFound(file)
}
