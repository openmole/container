package container

import container.ImageBuilder.checkImageFile
import scala.sys.process._
import java.io.File


object ContainerExecutor {

  def executeContainerWithDocker(image: BuiltDockerImage, command: Option[Seq[String]] = None) = {
    checkImageFile(image.file)
    val file = image.file.getAbsolutePath
    ("docker load -i " +  file) !!

    val commandValue = command.getOrElse(image.command)
    Seq("docker", "run", image.imageName) ++ commandValue !!
  }

  def executeContainerWithPRoot(proot: File, image: BuiltPRootImage, command: Option[Seq[String]] = None) = {
    checkImageFile(image.file)

    val path = image.file.getAbsolutePath + "/"
    (Seq(proot.getAbsolutePath, "-r", path + "rootfs/") ++ command.getOrElse(image.command)) .!!
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
