package container

import container.ImageBuilder.checkImageFile
import scala.sys.process._
import container.Status.note
import better.files.{File => BFile, _}


object ContainerExecutor {

//  def executeContainerWithDocker(image: SavedDockerImage): Unit = {

    def executeContainerWithDocker(image: BuiltDockerImage): Unit = {
    note(" - execute container with Docker")
    note(" -- checkImage")
    checkImageFile(image.file)
    val file = image.file.toScala.pathAsString

      println(s"executeDocker : file: $file")

    ("docker load -i" +  file).!!


    note("cmd = " + image.command.mkString("'", " ", "'"))

    if (image.command.isEmpty)
      ("docker run " + image.imageName).!!
    else {
      val x = ("docker run " + image.imageName + " sh -c "+ image.command.mkString(""," ", "")) .!
      println(x)
    }
//      ("docker run " + image.imageName + " sh -c "+ image.command.mkString("'"," ", "'")) .!

    }

  def executeContainerWithoutDocker(image: BuiltPRootImage): Unit = {
    note(" - execute container without Docker")
    note(" -- checkImage")
    checkImageFile(image.file)
    val path = image.file.getAbsolutePath + "/"
    note(" -- prepareEnvVar")

    prepareEnvVariables(image.configurationData.Env)
    note(" -- run proot")

    note("cmd = " + image.command)
    println("pRoot cmd args:\n" + "proot -r " + path + "rootfs/")
    val status = Seq("proot", "-r", path + "rootfs/").union(image.command) .!!
    println("status = " + status)
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
