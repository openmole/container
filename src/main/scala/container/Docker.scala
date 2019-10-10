package container

import java.io.File
import java.util.UUID

import container.ImageBuilder.checkImageFile

import scala.sys.process._

/*
 * Copyright (C) 2019 Romain Reuillon
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
object Docker {

  case class BuiltDockerImage(
    file: File,
    imageId: String,
    //   configurationData: ConfigurationData,
    command: Seq[String] = Seq())

  def build(image: SavedImage, archive: File, dockerCommand: String = "docker"): BuiltDockerImage = {
    //if (image.compressed) BuiltDockerImage(image.file, image.imageName, image.command)
    //val path = image.file.toScala.pathAsString
    //BFile(path + "/manifest.json").delete()
    Tar.archive(image.file, archive)
    checkImageFile(image.file)

    val imageId = UUID.randomUUID().toString

    val file = archive.getAbsolutePath

    val out = (s"$dockerCommand load -i $file").!!
    val name = out.split("\n").head.split(":").drop(1).mkString(":").trim
    (s"$dockerCommand tag ${name} $imageId").!!

    BuiltDockerImage(archive, imageId, image.command)
  }

  def execute(image: BuiltDockerImage, command: Option[Seq[String]] = None, dockerCommand: String = "docker") =
    Seq(dockerCommand, "run", "--name", image.imageId, image.imageId) ++ command.getOrElse(image.command) !!

  def clean(image: BuiltDockerImage, dockerCommand: String = "docker") = {
    (s"$dockerCommand rm ${image.imageId}").!!
    (s"$dockerCommand rmi ${image.imageId}").!!
    image.file.delete()
  }

}
