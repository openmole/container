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
import java.io.{ File, PrintStream }
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

  def buildSIF(image: FlatImage, sif: File, tmpDirectory: File, singularityCommand: String = "singularity", logger: PrintStream = tool.outputLogger) = {
    import better.files._

    val id = UUID.randomUUID().toString

    val buildDirectory = tmpDirectory.toScala / id
    buildDirectory.createDirectoryIfNotExists(createParents = true)

    try {
      def volumes =
        (image.file.toScala / FlatImage.rootfsName).list.filter(f => !Set("proc", "dev", "run").contains(f.name)).map {
          f => s"${f.toJava.getAbsolutePath}" -> s"${f.toJava.getName}"
        }

      (buildDirectory / "empty.def").writeText(
        s"""
           |Bootstrap: scratch
           |
           |%files
           |${volumes.map { v => s"\t ${v._1} ${v._2}" }.mkString("\n")}
           |""".stripMargin)

      val sandbox = (buildDirectory / "empty")

      ProcessUtil.execute(
        Seq(singularityCommand, "build", "--fakeroot", sandbox.toJava.getAbsolutePath, (buildDirectory / "empty.def").toJava.getAbsolutePath),
        logger,
        logger)

    } finally buildDirectory.delete()
  }

  def executeFlatImage(
    image: FlatImage,
    tmpDirectory: File,
    commands: Seq[String] = Seq.empty,
    singularityCommand: String = "singularity",
    bind: Seq[(String, String)] = Vector.empty,
    workDirectory: Option[String] = None,
    environmentVariables: Seq[(String, String)] = Vector.empty,
    useFakeroot: Boolean = false,
    singularityWorkdir: Option[File] = None,
    output: PrintStream = tool.outputLogger,
    error: PrintStream = tool.outputLogger) =
    import better.files._

    val id = UUID.randomUUID().toString

    val buildDirectory = tmpDirectory.toScala / id
    buildDirectory.createDirectoryIfNotExists(createParents = true)

    try
      def variables =
        image.env.getOrElse(Seq.empty).map { e =>
          val name = e.takeWhile(_ != '=')
          val value = e.dropWhile(_ != '=').drop(1)
          (name, value)
        } ++ environmentVariables

      val cmd =
        s"""
         |${variables.map { case (n, v) => s"""export $n="$v"""" }.mkString("\n")}
         |${(if (commands.isEmpty) image.command.toSeq else commands).mkString("&& \\" + "\n")}
        """.stripMargin

      val runFile = "_run_commands.sh"

      (buildDirectory / runFile).writeText(cmd)
      (buildDirectory / runFile).toJava.setExecutable(true)


      def wd =
        def emptyIsRoot(path: String) =
          path match
            case "" => "/"
            case s => s

        workDirectory.orElse(image.workDirectory).map(emptyIsRoot)

      def pwd =
        wd.map(w => Seq("--pwd", w)).getOrElse(Seq.empty)

      def fakeroot = if (useFakeroot) Seq("--fakeroot") else Seq()
      def singularityWorkdirArgument =
        singularityWorkdir match
          case Some(w) => Seq("--workdir", w.getAbsolutePath)
          case None => Seq()

      val absoluteRootFS = (image.file.toScala / FlatImage.rootfsName).toJava.getAbsolutePath
      def touchContainerFile(f: String, directory: Boolean) =
        val localFile = new java.io.File((image.file.toScala / FlatImage.rootfsName).toJava, f)
        if !localFile.exists()
        then
          if !directory
          then
            localFile.getParentFile.mkdirs()
            localFile.toScala.touch()
          else localFile.mkdirs()

      touchContainerFile(runFile, false)

      val absoluteBind =
        bind.map: (l, d) =>
          val nd =
            if java.io.File(d).isAbsolute
            then d
            else
              wd match
                case Some(wd) => java.io.File(wd, d).getAbsolutePath
                case None => java.io.File("/", d).getAbsolutePath
          l -> nd

      absoluteBind foreach { (l, d) => touchContainerFile(d, new java.io.File(l).isDirectory) }

      // Create directory requiered by singularity
      def createDirectories() =
        def relativeWd = wd.toSeq.map(_.dropWhile(_.isSpaceChar).dropWhile(_ == '/'))
        (Seq("dev", "root", "tmp", "var/tmp") ++ relativeWd).foreach { dir => (image.file.toScala / FlatImage.rootfsName / dir) createDirectoryIfNotExists (createParents = true) }

      createDirectories()

      ProcessUtil.execute(
        Seq(
          singularityCommand,
          "--silent",
          "exec",
          "--cleanenv",
          "-w") ++
          fakeroot ++
          singularityWorkdirArgument ++
          pwd ++
          Seq(
            "--home", s"$absoluteRootFS/root:/root",
            "-B", s"$absoluteRootFS/tmp:/tmp",
            "-B", s"$absoluteRootFS/var/tmp:/var/tmp") ++ absoluteBind.flatMap { case (f, t) => Seq("-B", s"$f:$t") } ++
            Seq("-B", s"${(buildDirectory / runFile).toJava.getAbsolutePath}:/$runFile") ++
            Seq(absoluteRootFS, "sh", s"/$runFile"),
        env = ProcessUtil.environmentVariables.filter(_._1 != "SINGULARITY_BINDPATH"),
        out = output,
        err = error)

      // TODO copy new directories at the root in the sandbox back to rootfs ?
    finally buildDirectory.delete()

}
