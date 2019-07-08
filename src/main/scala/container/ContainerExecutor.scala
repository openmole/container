package container

import container.ImageBuilder.checkImageFile
import scala.sys.process._
import java.io.File


object ContainerExecutor {
    def executeContainerWithDocker(image: BuiltDockerImage): Unit = {
      println(image.file)
      checkImageFile(image.file)
      val file = image.file.getAbsolutePath

      println("docker load -i " +  file)
      ("docker load -i " +  file).!!

      if (image.command.isEmpty) ("docker run " + image.imageName).!!
      else ("docker run " + image.imageName + " sh -c "+ image.command.mkString(""," ", "")) .!
    }

  def executeContainerWithoutDocker(image: BuiltPRootImage)(binPath: File): Unit = {
    checkImageFile(image.file)
    val path = image.file.getAbsolutePath + "/"
    prepareEnvVariables(image.configurationData.Env)
    val status = Seq(binPath.getAbsolutePath + "proot", "-r", path + "rootfs/").union(image.command) .!!
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
