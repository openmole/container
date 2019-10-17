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

  //File("/tmp/extract").delete(swallowIOExceptions = true)
  //val saved = ImageBuilder.extractImage(File("/tmp/debian.tar").toJava, File("/tmp/extract").toJava)

  //File("/tmp/docker-repo").delete(swallowIOExceptions = true)
  File("/tmp/container").delete(swallowIOExceptions = true)

  val saved =
    ImageDownloader.downloadContainerImage(RegistryImage("debian"), File("/tmp/docker-repo/").toJava, 1 minutes, executor = ImageDownloader.Executor.parallel)

  val buildProot = Proot.buildImage(saved, File("/tmp/container/").toJava)
  /*print(
    Proot.execute(
      buildProot,
      File("/tmp/").toJava,
      Seq("/bin/ls"))
      */

  Docker.executeProotImage(
    buildProot,
    File("/tmp/dock").toJava,
    Seq("/bin/ls"))

  /*,
      bind = Vector("/tmp/youpi" -> "/home/youpi"),
      workDirectory = Some("/home")))*/

  //val built = CharlieCloud.buildImage(saved, File("/tmp/container/").toJava)
  //print(CharlieCloud.execute(File("/tmp/ch-run").toJava, built, Some(Seq("/bin/ls", "/"))))

  //  File("/tmp/extract").delete(swallowIOExceptions = true)
  //  val saved = ImageBuilder.extractImage(File("/tmp/debian.tar").toJava, File("/tmp/extract").toJava)
  //  val build = Docker.build(saved, new java.io.File("/tmp/fake.tar"))
  //  try print(Docker.execute(build, Some(Seq("/bin/ls"))))
  //  finally Docker.clean(build)

  //val build = Singularity.build(saved, new java.io.File("/tmp/fake.simg"))
  //print(Singularity.execute(build, Some(Seq("/bin/ls", "/"))))
  //finally Docker.clean(build)

}
