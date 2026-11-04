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

object ImageBuilder:
  case class FileNotFound(file: File) extends Exception
  case class InvalidImage(file: File) extends Exception
  case class DirectoryFileCollision(file: File) extends Exception
  case class CommandExecutionError(status: Int, stdout: String, stderr: String) extends Exception

  def extractImage(file: File, extractDirectory: File, compressed: Boolean = false): SavedImage =
    if !isAnArchive(file.getAbsolutePath) then throw InvalidImage(file)
    extractDirectory.mkdirs()
    Tar.extract(file, extractDirectory, compressed = compressed)
    SavedImage(extractDirectory)

  def copyIntoFlatImage(file: java.io.File, image: FlatImage, destination: String): Unit =
    val destinationFile = java.io.File(FlatImage.root(image), destination)
    container.tool.copyOrMerge(file.toPath, destinationFile.toPath)

  def flattenImage(image: SavedImage, workDirectory: java.io.File): FlatImage =
    case class Layer(path: String, zipped: Boolean)

    def extractLayers(savedImage: SavedImage, layers: Seq[Layer], destination: File) =
      destination.toScala.createDirectoryIfNotExists()

      layers.zipWithIndex.foreach: (l, i) =>
        Tar.extract((savedImage.file.toScala / l.path).toJava, destination, compressed = l.zipped, filter = Some(e => OCI.WhiteoutUtils.isWhiteout(java.nio.file.Paths.get(e.getName))))
        removeWhiteouts(destination)
        Tar.extract((savedImage.file.toScala / l.path).toJava, destination, compressed = l.zipped, filter = Some(e => !OCI.WhiteoutUtils.isWhiteout(java.nio.file.Paths.get(e.getName))))

    def extractV1 =
      val manifest = Registry.decodeTopLevelManifest((image.file.toScala / "manifest.json").contentAsString).get
      val config = Registry.decodeConfig(image.file.toScala / manifest.Config contentAsString).get

      val rootfs = workDirectory.toScala / FlatImage.rootfsName
      extractLayers(image, manifest.Layers.map(l => Layer(l, false)), rootfs.toJava)

      FlatImage(
        file = workDirectory,
        workDirectory = Registry.Config.workDirectory(config),
        env = Registry.Config.env(config),
        layers = manifest.Layers)

    def extractOCI(architecture: String = "amd64") =
      val index = Registry.decodeOCIIndex((image.file.toScala / "index.json").contentAsString).get

      val blobIndex =
        val d = DockerMetadata.OCI.parseDigest(index.manifests.head.digest)
        Registry.decodeOCIIndex((image.file.toScala / "blobs" / d.hashType / d.hash).contentAsString).get

      val manifest =
        val descriptor = blobIndex.manifests.find(_.platform.map(_.architecture).contains(architecture)).head
        val d = DockerMetadata.OCI.parseDigest(descriptor.digest)
        Registry.decodeManifestSchema2((image.file.toScala / "blobs" / d.hashType / d.hash).contentAsString).get

      val layers =
        manifest.layers.map: l =>
          val d = DockerMetadata.OCI.parseDigest(l.digest)
          val path = s"blobs/${d.hashType}/${d.hash}"
          l.mediaType.split('/').last.split('.').last match
            case "tar+gzip" => Layer(path, true)
            case _ => Layer(path, false)

      val rootfs = workDirectory.toScala / FlatImage.rootfsName

      extractLayers(image, layers, rootfs.toJava)

      val topLevelManifest = Registry.decodeTopLevelManifest((image.file.toScala / "manifest.json").contentAsString).get
      val config = Registry.decodeConfig(image.file.toScala / topLevelManifest.Config contentAsString).get

      FlatImage(
        file = workDirectory,
        workDirectory = Registry.Config.workDirectory(config),
        env = Registry.Config.env(config),
        layers = topLevelManifest.Layers)

    if (image.file.toScala / "index.json").exists()
    then extractOCI()
    else extractV1

  def duplicateFlatImage(flatImage: FlatImage, directory: java.io.File) =
    FlatImage(
      file = flatImage.file.toScala.copyTo(directory.toScala)(BFile.CopyOptions(overwrite = true) ++ BFile.LinkOptions.noFollow).toJava,
      workDirectory = flatImage.workDirectory,
      env = flatImage.env,
      layers = flatImage.layers,
      command = flatImage.command)

  def checkImageFile(file: File): Unit = if (!file.exists()) throw FileNotFound(file)

