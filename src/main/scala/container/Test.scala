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

  val image = RegistryImage("alpine")

  //File("/tmp/docker-repo").delete(swallowIOExceptions = true)
  File("/tmp/container").delete(swallowIOExceptions = true)

  val saved = ImageDownloader.downloadContainerImage(image, File("/tmp/docker-repo/").toJava, 1 minutes)

  //val buildProot = Proot.buildImage(saved, File("/tmp/container/").toJava)
  //print(Proot.execute(File("/tmp/proot").toJava, buildProot, Some(Seq("/bin/ls", "/"))))

  //val built = CharlieCloud.buildImage(saved, File("/tmp/container/").toJava)
  //print(CharlieCloud.execute(File("/tmp/ch-run").toJava, built, Some(Seq("/bin/ls", "/"))))


  val build = Docker.build(saved, new java.io.File("/tmp/fake.tar"))
   print(Docker.execute(build, Some(Seq("/bin/ls"))))
  //finally Docker.clean(build)

}
