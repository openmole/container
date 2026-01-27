import java.io.File

import java.io.File
import container.OCI.ConfigurationData

import scala.sys.process.Process

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
package object container:

  object FlatImage:
    val rootfsName = "rootfs"

    def root(flatImage: FlatImage) =
      import better.files.*
      (flatImage.file.toScala / rootfsName).toJava
    
    def file(flatImage: FlatImage, path: String) =
      import better.files.*
      (root(flatImage).toScala / path).toJava

  case class FlatImage(
    file: File,
    workDirectory: Option[String],
    env: Option[Seq[String]],
    layers: Seq[String],
    command: Option[String] = None)

  case class SavedImage(file: File)
