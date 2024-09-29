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

//  File("/tmp/docker-repo").delete(swallowIOExceptions = true)
//  File("/tmp/container").delete(swallowIOExceptions = true)

  //val saved = ImageDownloader.downloadContainerImage(RegistryImage("julia", "1.9.4"), File("/tmp/docker-repo/").toJava, 1 minutes, executor = ImageDownloader.Executor.parallel)
  val saved = ImageDownloader.downloadContainerImage(RegistryImage("python", "3.10.2"), File("/tmp/docker-repo/").toJava, 1 minutes, executor = ImageDownloader.Executor.parallel)

  //val saved = ImageBuilder.extractImage(File("/tmp/test.tar").toJava, File("/tmp/extract").toJava)

  val flattenedImage = ImageBuilder.flattenImage(saved, File("/tmp/container/").toJava)

  //  Proot.execute(
  //    flattenedImage,
  //    File("/tmp/").toJava,
  //    commands = Seq("ls"),
  //    workDirectory = Some("/tmp"),
  //    bind = Seq("/tmp/test" -> "/tmp/test"))

  val sifImage = File("/tmp/image.sif")
  val buildSif = Singularity.buildSIF(flattenedImage, sifImage.toJava)

  val overlayFile = File("/tmp/overlay.img")
  val overlay = Singularity.createOverlay(overlayFile.toJava)

  Singularity.executeImage(buildSif, File("/tmp/container").toJava, overlay = Some(overlay), commands = Seq("rm /var/lib/python/python3.9_installed", "ls -la /var/lib/python/"))

  Singularity.executeImage(buildSif, File("/tmp/container").toJava, overlay = Some(overlay), commands = Seq("echo 2", "ls -la /var/lib/python/"))

  Singularity.clearOverlay(overlay = overlay, File("/tmp/overlay").toJava)

  Singularity.executeImage(buildSif, File("/tmp/container").toJava, overlay = Some(overlay), commands = Seq("echo 3", "ls -la /var/lib/python/"))

  //  Singularity.executeFlatImage(
//    flattenedImage,
//    File("/tmp/").toJava,
//    Seq("apt update"))

  //  Docker.executeFlatImage(
  //    flattenedImage,
  //    File("/tmp/dock").toJava,
  //    Seq("/bin/ls", "/bin/ls -l"))

  //  Singularity.executeFlatImage(
  //    flattenedImage,
  //    File("/tmp/sing").toJava,
  //    Seq("touch /test", "/bin/ls", "/bin/ls -l"),
  //    workDirectory = Some("/"))

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
