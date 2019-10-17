package container

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

import container.ImageBuilder.checkImageFile
import container.OCI.ConfigurationData
import java.io.File
import scala.sys.process._
import better.files._

object Singularity {

  case class BuiltSingularityImage(
    file: File,
    command: Seq[String] = Seq())

  //  def buildImage(image: SavedImage, workDirectory: File): BuiltSingularityImage = {
  //    val preparedImage = ImageBuilder.prepareImage(ImageBuilder.extractImage(image, workDirectory))
  //    ImageBuilder.buildImage(preparedImage, workDirectory)
  //    BuiltSingularityImage(workDirectory, preparedImage.configurationData, preparedImage.command)
  //  }

  def build(image: SavedImage, archive: File, singularityCommand: String = "singularity"): BuiltSingularityImage = {
    //if (image.compressed) BuiltDockerImage(image.file, image.imageName, image.command)
    //val path = image.file.toScala.pathAsString
    //BFile(path + "/manifest.json").delete()

    val tarArchive = {
      val tmp = archive.toScala.parent / (archive.getName + ".tar")
      Tar.archive(image.file, tmp.toJava)
      tmp.toJava
    }

    try {
      (s"$singularityCommand build ${archive.getAbsolutePath} docker-archive://${tarArchive.getAbsolutePath}") !!

      BuiltSingularityImage(archive, image.command)
    } finally tarArchive.delete()
  }

  def execute(image: BuiltSingularityImage, command: Option[Seq[String]] = None, singularityCommand: String = "singularity") = {
    val file = image.file.getAbsolutePath
    Seq(singularityCommand, "run", file) ++ command.getOrElse(image.command) !!
  }

}
