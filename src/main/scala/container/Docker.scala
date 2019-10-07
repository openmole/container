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
    imageName: String,
    //   configurationData: ConfigurationData,
    command: Seq[String] = Seq())

  def build(image: SavedImage, archive: File, dockerCommand: String = "docker"): BuiltDockerImage = {
    if (image.compressed) BuiltDockerImage(image.file, image.imageName, image.command)
    //val path = image.file.toScala.pathAsString
    //BFile(path + "/manifest.json").delete()
    Tar.archive(image.file, archive)
    checkImageFile(image.file)

    val imageName = UUID.randomUUID().toString

    val file = archive.getAbsolutePath

    (s"$dockerCommand load -i $file").!!
    (s"$dockerCommand tag ${image.imageName} $imageName").!!

    BuiltDockerImage(archive, imageName, image.command)
  }

  def execute(image: BuiltDockerImage, command: Option[Seq[String]] = None, dockerCommand: String = "docker") =
    Seq(dockerCommand, "run", "--name", image.imageName, image.imageName) ++ command.getOrElse(image.command) !!

  def clean(image: BuiltDockerImage, dockerCommand: String = "docker") = {
    (s"$dockerCommand rm ${image.imageName}").!!
    (s"$dockerCommand rmi ${image.imageName}").!!
    image.file.delete()
  }

}
