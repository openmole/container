//package container
//
///*
// * Copyright (C) 2019 Romain Reuillon
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU Affero General Public License for more details.
// *
// * You should have received a copy of the GNU Affero General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
//
//import container.ImageBuilder.checkImageFile
//import container.OCI.ConfigurationData
//import java.io.File
//import scala.sys.process._
//
//object Singularity {
//
//  case class BuiltSingularityImage(
//    file: File,
//    configurationData: ConfigurationData,
//    command: Seq[String] = Seq())
//
//  def buildImage(image: SavedImage, workDirectory: File): BuiltSingularityImage = {
//    val preparedImage = ImageBuilder.prepareImage(ImageBuilder.extractImage(image, workDirectory))
//    ImageBuilder.buildImage(preparedImage, workDirectory)
//    BuiltSingularityImage(workDirectory, preparedImage.configurationData, preparedImage.command)
//  }
//
//  def execute(singularity: File, image: BuiltSingularityImage, command: Option[Seq[String]] = None) = {
//    checkImageFile(image.file)
//    val file = image.file.getAbsolutePath
//    Seq(singularity.getPath, file) ++ command.getOrElse(image.command) !!
//  }
//
//}
