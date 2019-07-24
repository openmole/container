/*
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

import container.ImageBuilder.checkImageFile
import scala.sys.process._
import java.io.File


object ContainerExecutor {
  def executeContainerWithDocker(image: BuiltDockerImage): Unit = {
    checkImageFile(image.file)
    val file = image.file.getAbsolutePath
    ("docker load -i " +  file).!!
    if (image.command.isEmpty) ("docker run " + image.imageName).!!
    else ("docker run " + image.imageName + " sh -c "+ image.command.mkString(""," ", "")) .!
  }

  def executeContainerWithPRoot(proot: File, image: BuiltPRootImage, command: Option[Seq[String]] = None): Unit = {
    checkImageFile(image.file)

    val path = image.file.getAbsolutePath + "/"
    val status = (Seq(proot.getAbsolutePath, "-r", path + "rootfs/") ++ command.getOrElse(image.command)) .!!

    println(status)
  }

  def prepareEnvVariables(maybeArgs: Option[List[String]]) {
    maybeArgs match {
      case Some(args)     =>
        for (arg <- args)
        {
          val s = arg.split("=")
          if (s.length == 2) Process("bash", None, s(0) -> s(1)) !!
        }
      case _              =>
    }
  }
}
