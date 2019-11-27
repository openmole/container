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

import container.ImageBuilder.checkImageFile
import container.OCI.ConfigurationData
import java.io.File
import java.util.UUID

import scala.sys.process._
import better.files._
import container.tool.Tar

object Singularity {

  case class BuiltSingularityImage(
    file: File,
    command: Seq[String] = Seq())

  //  def buildImage(image: SavedImage, workDirectory: File): BuiltSingularityImage = {
  //    val preparedImage = ImageBuilder.prepareImage(ImageBuilder.extractImage(image, workDirectory))
  //    ImageBuilder.buildImage(preparedImage, workDirectory)
  //    BuiltSingularityImage(workDirectory, preparedImage.configurationData, preparedImage.command)
  //  }

  def build(image: SavedImage, archive: File, singularityCommand: String = "singularity"): BuiltSingularityImage = {
    //if (image.compressed) BuiltDockerImage(image.file, image.imageName, image.command)
    //val path = image.file.toScala.pathAsString
    //BFile(path + "/manifest.json").delete()

    val tarArchive = {
      val tmp = archive.toScala.parent / (archive.getName + ".tar")
      Tar.archive(image.file, tmp.toJava)
      tmp.toJava
    }

    try {
      (s"$singularityCommand build ${archive.getAbsolutePath} docker-archive://${tarArchive.getAbsolutePath}") !!

      BuiltSingularityImage(archive)
    } finally tarArchive.delete()
  }

  def execute(image: BuiltSingularityImage, command: Option[Seq[String]] = None, singularityCommand: String = "singularity") = {
    val file = image.file.getAbsolutePath
    Seq(singularityCommand, "run", file) ++ command.getOrElse(image.command) !!
  }

  def executeFlatImage(
    image: FlatImage,
    tmpDirectory: File,
    commands: Seq[String] = Seq.empty,
    singularityCommand: String = "singularity",
    bind: Seq[(String, String)] = Vector.empty,
    workDirectory: Option[String] = None,
    environmentVariables: Seq[(String, String)] = Vector.empty,
    logger: ProcessLogger = tool.outputLogger) = {
    import better.files._

    val id = UUID.randomUUID().toString

    val buildDirectory = tmpDirectory.toScala / id
    buildDirectory.createDirectoryIfNotExists(createParents = true)

    try {
      val cmd = if (commands.isEmpty) image.command.toSeq else commands

      val runFile = "_run_commands.sh"

      (buildDirectory / runFile).writeText(cmd.mkString("\n"))
      (buildDirectory / runFile).toJava.setExecutable(true)

      (buildDirectory / "empty.def").writeText(
        s"""
          |Bootstrap: scratch
          |
          |%files
          |  ${(buildDirectory / runFile).toJava.getAbsolutePath} /$runFile
          |""".stripMargin)

      val sandbox = (buildDirectory / "empty")
      Seq(singularityCommand, "build", "--fakeroot", "--sandbox", sandbox.toJava.getAbsolutePath, (buildDirectory / "empty.def").toJava.getAbsolutePath) !! logger

      def volumes =
        (image.file.toScala / FlatImage.rootfsName).list.filter(f => !Set("proc", "dev", "run").contains(f.name)).flatMap {
          f => Seq("-B", s"${f.toJava.getAbsolutePath}:/${f.toJava.getName}")
        } ++ (bind ++ Seq(("/proc", "/proc"))).flatMap { b => Seq("-B", s"""${b._1}:${b._2}""") }

      def variables =
        image.env.getOrElse(Seq.empty).map { e =>
          val name = e.takeWhile(_ != '=')
          val value = e.dropWhile(_ != '=').drop(1)
          (s"SINGULARITY_$name", value)
        } ++ environmentVariables.map { e =>
          (s"SINGULARITY_${e._1}", e._2)
        }

      def pwd = workDirectory.map(w => Seq("--pwd", w)).getOrElse(Seq.empty)

      Process(
        Seq(
          singularityCommand,
          "exec",
          "-W",
          buildDirectory.toJava.getAbsolutePath,
          "--cleanenv",
          "--fakeroot",
          "--containall") ++ pwd ++ volumes ++ Seq(sandbox.toJava.getAbsolutePath, s"/$runFile"), None, extraEnv = variables: _*) ! logger

      // TODO copy new directories at the root in the sandbox back to rootfs ?
    } finally buildDirectory.delete()
  }

}
