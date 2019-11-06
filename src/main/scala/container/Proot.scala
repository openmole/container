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

import java.io.{ File, PrintWriter }

import container.ImageBuilder.{ checkImageFile }
import container.OCI._
import container.Status._
import better.files._

import scala.sys.process._

object Proot {

  val scriptName = "launcher.sh"

  //    def loadImage(imagePath: String): Status = {
  //        // 1. We prepare the image by extracting it
  //        note("- preparing image")
  //        val directoryPath = prepareImage(imagePath) match {
  //            case (OK, Some(path))                 => path
  //            case (badStatus, _)             => return badStatus
  //        }
  //
  //        // 2. We explore the directory and start retrieving some info
  //        note("- analyzing image")
  //        val (manifestData, configData) = analyseImage(directoryPath) match {
  //            case (OK, Some(d1), Some(d2))   => (d1, d2)
  //            case (badStatus, _, _)          => return badStatus
  //        }
  //
  //        // 3. We squash the image by merging the image layers
  //        note("- squashing image")
  //        squashImage(directoryPath, manifestData.Layers) match {
  //            case OK                         =>
  //            case badStatus                  => return badStatus
  //        }
  //
  //        // 4. We prepare a bash script that will contain the required options for PRoot
  //        note("- preparing launcher script")
  //        generatePRootScript(directoryPath, Some(configData)) match {
  //            case OK                         =>
  //            case badStatus                  => return badStatus
  //        }
  //
  //        return OK
  //    }

  /**
   * Check that the image or directory exists, and extract it if it hasn't been already.
   * Return the status and the directory where the image has been extracted.
   */
  //    def prepareImage(imagePath: String): (Status, Option[String]) = {
  //        var status: Status = OK
  //        val file = new File(imagePath)
  //
  //        // first, we check that the path leads to somewhere
  //        if (!file.exists) return (INVALID_PATH.withArgument(imagePath), None)
  //
  //        // then, either we're given an archive, or a directory
  //        if (!file.isDirectory) {
  //            val (path, archiveName) = getPathAndFileName(imagePath)
  //            if(!isAnArchive(archiveName))
  //                return (INVALID_IMAGE.withArgument(archiveName + " is not a valid archive file."), None)
  //
  //            val directoryPath = getDirectoryPath(path, archiveName)
  //            status = createDirectory(directoryPath)
  //            if (status.isBad)
  //                return (status, None)
  //
  //            note("-- extracting archive")
  //            status = extractArchive(path + archiveName, directoryPath)
  //            if (status.isBad)
  //                return (status, None)
  //
  //            return (OK, Some(completeDirectoryPath(directoryPath)))
  //        }
  //        else {
  //            note("-- already extracted")
  //            return (OK, Some(completeDirectoryPath(imagePath)))
  //        }
  //    }

  /**
   * Retrieve metadata (layer ids, env variables, volumes, ports, commands)
   * from the manifest and configuration files.
   */
  //    def analyseImage(directoryPath: String): (Status, Option[ManifestData], Option[ConfigurationData]) = {
  //        val rootDirectory = new File(directoryPath)
  //
  //        assert(rootDirectory.exists)
  //
  //        note("-- extracting manifest data")
  //
  //        val manifest = getManifestFile(rootDirectory.list) match {
  //            case None       => return (INVALID_IMAGE_FORMAT.withArgument("No manifest.json file in the root directory."), None, None)
  //            case Some(path) => new File(directoryPath + path)
  //        }
  //
  //        val manifestLines = extractLines(manifest)
  //        val manifestData = harvestManifestData(manifestLines)
  //        note("manifestData: " + manifestData)
  //
  //        val config = new File(directoryPath + manifestData.Config)
  //        if (!config.exists) return (INVALID_IMAGE_FORMAT.withArgument("No configuration json file in the root directory."), None, None)
  //
  //        note("-- extracting configuration data")
  //
  //        val configLines = extractLines(config)
  //        val configData = harvestConfigData(configLines)
  //
  //        return (OK, Some(manifestData), Some(configData))
  //    }
  //

  /**
   * Merge the layers by extracting them in order in a same directory.
   * Also, delete the whiteout files.
   */
  //    def squashImage(directoryPath: String, layers: List[String]): Status = {
  //        val rootfsPath = directoryPath + rootfsName
  //        createDirectory(rootfsPath) match {
  //            case OK         =>
  //            case status     => return status
  //        }
  //
  //        layers.foreach{
  //            layerName => {
  //                note("-- extracting layer " + layerName)
  //                extractArchive(directoryPath + layerName, rootfsPath) match {
  //                    case OK         =>
  //                    case status     => return status
  //                }
  //             }
  //        }
  //
  //        note("-- removing whiteouts")
  //        removeWhiteouts(rootfsPath) match {
  //            case OK         =>
  //            case status     => return status
  //        }
  //
  //        return OK
  //    }

  val standardVarsFuncName = "prepareStandardVars"
  val envFuncName = "prepareEnv"
  val printCommandsFuncName = "printCommands"
  val infoVolumesFuncName = "infoVolumes"
  val infoPortsFuncName = "infoPorts"
  val runPRootFuncName = "runPRoot"
  val bashVarPrefix = "LOADER_"
  val workdirBashVAR = bashVarPrefix + "WORKDIR"
  val entryPointBashVar = bashVarPrefix + "ENTRYPOINT"
  val cmdBashVar = bashVarPrefix + "CMD"
  val launchScriptName = "launch.sh"

  object Script {
    //    def prepareVariables(args: Seq[String], functionName: String, write: String => Unit) = {
    //      write("function " + functionName + " {")
    //      for (arg <- args) write("\t" + addQuoteToRightSideOfEquality(arg))
    //      write("}\n")
    //    }

    def prepareEnvVariables(maybeArgs: Option[Seq[String]], functionName: String, write: String => Unit) = {
      write("function " + functionName + " {")
      maybeArgs match {
        case Some(args) => {
          for (arg <- args)
            write("\texport " + addQuoteToRightSideOfEquality(arg))
        }
        case _ => write("\t:")
      }
      write("}\n")
    }

    def prepareMapInfo(maybeMap: Option[Map[String, String]], title: String, functionName: String, write: String => Unit) = {
      write("function " + functionName + " {")
      write("\techo \"" + title + ":\"")
      maybeMap match {
        case Some(map) => for ((variable, _) <- map) write("\techo \"\t" + variable + "\"")
        case _ =>
      }
      write("}\n")
    }

    def assembleCommandParts(args: String*): String = {
      var command = ""
      for (arg <- args) command += arg + " "
      command
    }

    def assembleCommandParts(args: List[String]): String = assembleCommandParts(args: _*)

    def addQuoteToRightSideOfEquality(s: String) = {
      s.split('=') match {
        case Array(left, right) => left + "=\"" + right + "\""
        case _ => s
      }
    }
  }

  def generatePRootScript(
    scriptFile: java.io.File,
    proot: String,
    rootFS: String,
    workDirectory: Option[String],
    bind: Seq[(String, String)],
    containerEnvironmentVariables: Option[Seq[String]],
    environmentVariables: Seq[(String, String)],
    commandLines: Seq[String],
    noSeccomp: Boolean,
    kernel: Option[String]) = {

    //        val config = configInit match {
    //            case Some(conf) => conf
    //            case None       => {
    //                val (_, configData) = ImageBuilder.analyseImage(directory) match {
    //                    case (OK, Some(d1), Some(d2))   => (d1, d2)
    //                    case (badStatus, _, _)          => return badStatus
    //                }
    //                configData
    //            }
    //        }

    scriptFile.getParentFile.mkdirs()

    for {
      script <- scriptFile.toScala.newFileWriter().autoClosed // new PrintWriter(scriptFile)
    } {
      val writeln = (s: String) => script.write(s + "\n")

      writeln("#!/usr/bin/env bash\n")

      //      val workDir = config.WorkingDir match {
      //        case Some(s) if !s.isEmpty => s
      //        case _ => "/"
      //      }
      //
      //      val entryPoint = config.Entrypoint match {
      //        case Some(list) => assembleCommandParts(list)
      //        case _ => ""
      //      }
      //
      //      val cmd = config.Cmd match {
      //        case Some(list) => assembleCommandParts(list)
      //        case _ => ""
      //      }

      //Script.prepareVariables(environmentVariables.map { case(n, v) => s"$n=$v"}, standardVarsFuncName, writeln)

      //      prepareVariables(List(
      //        workdirBashVAR + "=" + workDir,
      //        entryPointBashVar + "=" + entryPoint,
      //        cmdBashVar + "=" + cmd), standardVarsFuncName, writeln)
      //
      Script.prepareEnvVariables(containerEnvironmentVariables, envFuncName, writeln)
      //
      //      prepareMapInfo(config.Volumes, "Data volumes", infoVolumesFuncName, writeln)
      //      prepareMapInfo(config.ExposedPorts, "Exposed ports", infoPortsFuncName, writeln)

      val noSeccompVariable = if (noSeccomp) Seq("PROOT_NO_SECCOMP" -> "1") else Seq()

      preparePRootCommand(
        writeln,
        proot,
        rootFS = rootFS,
        workDirectory = workDirectory,
        bind = bind,
        environmentVariables = environmentVariables ++ noSeccompVariable,
        commandLines = commandLines,
        kernel = kernel)
      prepareCLI(writeln)
    }

    scriptFile.setExecutable(true)
  }

  def prepareCLI(write: String => Unit) =
    write(s"""$runPRootFuncName $$1 "$$2" $${@:3}""".stripMargin)

  def preparePRootCommand(
    write: String => Unit,
    proot: String,
    rootFS: String,
    workDirectory: Option[String],
    bind: Seq[(String, String)],
    environmentVariables: Seq[(String, String)],
    commandLines: Seq[String],
    kernel: Option[String]) = {

    val workDirectoryArgs = workDirectory.filterNot(_.trim.isEmpty).map(w => s"-w $w").mkString(" ")
    val bindArgs = bind.map(b => s"""-b '${b._1}:${b._2}'""").mkString(" ")
    val kernelArg = kernel.map(k => s"-k $k").getOrElse("")

    write(
      s"""function $runPRootFuncName {
         |  PROOT=`which $proot`
         |  for i in $$(env | cut -d'=' -f1) ; do unset "$$i"; done
         |  ${environmentVariables.map { case (n, v) => s"export $n=$v" }.mkString("\n")}
         |  $envFuncName
         |  ${
        commandLines.map { commandLine =>
          Script.assembleCommandParts(
            "$PROOT", // calling PRoot
            "--kill-on-exit",
            "--netcoop",
            kernelArg,
            s"-r $rootFS", // setting guest rootfs
            workDirectoryArgs, // + workdirBashVAR, // setting working directory,
            bindArgs,
            commandLine // user inputs for the program
          )
        }.mkString("\n")
      }
         |}
         |""".stripMargin)
  }

  def execute(
    image: FlatImage,
    tmpDirectory: File,
    commands: Seq[String] = Vector.empty,
    proot: String = "proot",
    bind: Seq[(String, String)] = Vector.empty,
    workDirectory: Option[String] = None,
    environmentVariables: Seq[(String, String)] = Vector.empty,
    logger: ProcessLogger = tool.outputLogger,
    noSeccomp: Boolean = false,
    kernel: Option[String] = None) = {

    checkImageFile(image.file)

    val path = image.file.getAbsolutePath + "/"
    val rootFSPath = path + FlatImage.rootfsName

    val commandLines: Seq[String] = if (commands.isEmpty) Seq(image.command.mkString(" ")) else commands

    val script = (tmpDirectory.toScala / launchScriptName).toJava
    val workDirectoryValue = workDirectory.orElse(image.workDirectory)

    generatePRootScript(
      script,
      proot = proot,
      rootFS = rootFSPath,
      workDirectory = workDirectoryValue,
      bind = bind,
      containerEnvironmentVariables = image.env,
      environmentVariables = environmentVariables,
      commandLines = commandLines,
      noSeccomp = noSeccomp,
      kernel = kernel)

    try script.getAbsolutePath ! logger
    finally script.delete()
  }

}
