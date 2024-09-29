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

import container.ImageBuilder.{checkImageFile, extractImage}
import container.OCI.ConfigurationData

import java.io.{File, PrintStream}
import java.util.UUID
import scala.sys.process.*
import better.files.*
import container.tool.{Tar, outputLogger}
import squants.information.*

import java.nio.file.attribute.{PosixFilePermission, PosixFilePermissions}

object Singularity:

  case class SingularityImageFile(
    file: File,
    workDirectory: Option[String],
    env: Option[Seq[String]],
    layers: Seq[String],
    command: Option[String] = None)

  opaque type OverlayImg = File

  //  def buildImage(image: SavedImage, workDirectory: File): BuiltSingularityImage = {
  //    val preparedImage = ImageBuilder.prepareImage(ImageBuilder.extractImage(image, workDirectory))
  //    ImageBuilder.buildImage(preparedImage, workDirectory)
  //    BuiltSingularityImage(workDirectory, preparedImage.configurationData, preparedImage.command)
  //  }

//  def build(image: SavedImage, archive: File, singularityCommand: String = "singularity"): SingularityImageFile =
//    //if (image.compressed) BuiltDockerImage(image.file, image.imageName, image.command)
//    //val path = image.file.toScala.pathAsString
//    //BFile(path + "/manifest.json").delete()
//
//    val tarArchive =
//      val tmp = archive.toScala.parent / (archive.getName + ".tar")
//      Tar.archive(image.file, tmp.toJava)
//      tmp.toJava
//
//    try
//      s"$singularityCommand build ${archive.getAbsolutePath} docker-archive://${tarArchive.getAbsolutePath}".!!
//      SingularityImageFile(
//        archive)
//    finally tarArchive.delete()

//  def execute(image: SingularityImageFile, command: Option[Seq[String]] = None, singularityCommand: String = "singularity") =
//    val file = image.file.getAbsolutePath
//    Seq(singularityCommand, "run", file) ++ command.getOrElse(image.command) !!

  def buildSIF(
    image: FlatImage,
    sif: File,
    singularityCommand: String = "singularity",
    logger: PrintStream = tool.outputLogger): SingularityImageFile =

    val file = if !sif.getName.endsWith("sif") then java.io.File(sif.getParentFile, sif.getName + ".sif") else sif


    def setPermissions(f: java.io.File): Unit =
      import scala.jdk.CollectionConverters.*
      util.Try(java.nio.file.Files.getPosixFilePermissions(f.toPath)).map(_.asScala.toSet).foreach: permissions =>
        val permissionSet = scala.collection.mutable.Set[PosixFilePermission]()

        if permissions.contains(PosixFilePermission.OWNER_READ) then permissionSet ++= Seq(PosixFilePermission.OWNER_READ, PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ)
        if permissions.contains(PosixFilePermission.OWNER_WRITE) then permissionSet ++= Seq(PosixFilePermission.OWNER_WRITE, PosixFilePermission.GROUP_WRITE, PosixFilePermission.OTHERS_WRITE)
        if permissions.contains(PosixFilePermission.OWNER_EXECUTE) then permissionSet ++= Seq(PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_EXECUTE)

        util.Try(java.nio.file.Files.setPosixFilePermissions(f.toPath, permissionSet.asJava))

    import better.files.*
    val rootDirectory = image.file.toScala / FlatImage.rootfsName

    rootDirectory.listRecursively.foreach(f => setPermissions(f.toJava))

    ProcessUtil.execute(
      Seq(singularityCommand, "build", "--force", file.getAbsolutePath, rootDirectory.toJava.getAbsolutePath),
      logger,
      logger)

    SingularityImageFile(
      file = file,
      workDirectory = image.workDirectory,
      env = image.env,
      layers = image.layers,
      command = image.command
    )

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

      def pwd = wd.map(w => Seq("--pwd", w)).getOrElse(Seq.empty)

      def fakeroot = if useFakeroot then Seq("--fakeroot") else Seq()

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

      absoluteBind.foreach ((l, d) => touchContainerFile(d, new java.io.File(l).isDirectory))

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
            "-B", s"$absoluteRootFS/var/tmp:/var/tmp") ++
            absoluteBind.flatMap ((f, t) => Seq("-B", s"$f:$t")) ++
            Seq("-B", s"${(buildDirectory / runFile).toJava.getAbsolutePath}:/$runFile") ++
            Seq(absoluteRootFS, "sh", s"/$runFile"),
        env = ProcessUtil.environmentVariables.filter(_._1 != "SINGULARITY_BINDPATH"),
        out = output,
        err = error)

      // TODO copy new directories at the root in the sandbox back to rootfs ?
    finally buildDirectory.delete()


  def createOverlay(
    overlay: File,
    overlaySize: Information = Gibibytes(50),
    singularityCommand: String = "singularity",
    output: PrintStream = tool.outputLogger,
    error: PrintStream = tool.outputLogger): OverlayImg =
    overlay.getParentFile.mkdirs()
    overlay.delete()
    ProcessUtil.execute(Seq(singularityCommand, "overlay", "create", "-S", "-s", overlaySize.toMegabytes.intValue.toString, overlay.getAbsolutePath), out = output, err = error)
    overlay

  def clearOverlay(
    overlay: OverlayImg,
    tmpDirectory: File,
    output: PrintStream = tool.outputLogger,
    error: PrintStream = tool.outputLogger): OverlayImg =
    val overlayDirectory  = tmpDirectory.toScala / "overlay"
    try
      (overlayDirectory / "upper").createDirectories()
      (overlayDirectory / "work").createDirectories()

      ProcessUtil.execute(Seq("mkfs.ext3", "-F", "-d", overlayDirectory.toJava.getAbsolutePath, overlay.getAbsolutePath), out = output, err = error)
      overlay
    finally overlayDirectory.delete()

  def extractFile(
    image: SingularityImageFile,
    source: String,
    directory: File,
    tmpDirectory: File,
    overlay: Option[OverlayImg] = None) =
    val bindDirectory = s"/${UUID.randomUUID().toString}"
    def command = Seq(s"cp -rf $source $bindDirectory")
    executeImage(image, tmpDirectory, overlay, commands = command, bind = Seq(directory.getAbsolutePath -> bindDirectory))

  def executeImage(
    image: SingularityImageFile,
    tmpDirectory: File,
    overlay: Option[OverlayImg] = None,
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

      def pwd = wd.map(w => Seq("--pwd", w)).getOrElse(Seq.empty)
      def fakeroot = if useFakeroot then Seq("--fakeroot") else Seq()

      def singularityWorkdirArgument =
        singularityWorkdir match
          case Some(w) => Seq("--workdir", w.getAbsolutePath)
          case None => Seq()

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

      def overlayOption: Seq[String] =
        overlay.toSeq.flatMap: o =>
          Seq("--overlay", o.getAbsolutePath)

      ProcessUtil.execute(
        Seq(
          singularityCommand,
          "--silent",
          "exec",
          "--cleanenv",
          "--no-home") ++
          overlayOption ++
          fakeroot ++
          singularityWorkdirArgument ++
          pwd ++
          absoluteBind.flatMap((f, t) => Seq("-B", s"$f:$t")) ++
          Seq("-B", s"${(buildDirectory / runFile).toJava.getAbsolutePath}:/$runFile") ++
          Seq(image.file.getAbsolutePath, "sh", s"/$runFile"),
        env = ProcessUtil.environmentVariables.filter(_._1 != "SINGULARITY_BINDPATH"),
        out = output,
        err = error)

    finally buildDirectory.delete()



