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

  val image = DockerImage("openmole/fake")
  
  File("/tmp/docker-repo").delete(swallowIOExceptions = true)

  val saved = ImageDownloader.downloadContainerImage(image, File("/tmp/docker-repo/").toJava, 1 minutes)
//  val build = ImageBuilder.buildImageForProot(saved, File("/tmp/proot-work/").toJava)
 // ContainerExecutor.executeContainerWithPRoot(File("/tmp/proot").toJava, build, Some(Seq("/bin/ls")))
  
  val build = ImageBuilder.buildImageForDocker(saved, new java.io.File("/tmp/fake.tar"))
  print(ContainerExecutor.executeContainerWithDocker(build, Some(Seq("/bin/ls"))))

}
