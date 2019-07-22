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

import better.files._

object Test extends App {

  import squants.time.TimeConversions._

  val image = DockerImage("hello-world")
  
  File("/tmp/test").delete(swallowIOExceptions = true)

  val saved = ImageDownloader.downloadContainerImage(image, new java.io.File("/tmp/test"), 1 minutes)
  val build = ImageBuilder.buildImageForDocker(saved, new java.io.File("/tmp/hello.tar"))
  ContainerExecutor.executeContainerWithDocker(build)

}
