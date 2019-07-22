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
