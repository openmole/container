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


import java.io.{File, PrintWriter}

import container.ImageBuilder.checkImageFile
import container.OCI._
import container.Status._
import better.files._
import scala.sys.process._

object Proot {

  case class BuiltPRootImage(file: File,
                             configurationData: ConfigurationData,
                             command: Seq[String] = Seq())

  val rootfsName = "rootfs"
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


  /** Check that the image or directory exists, and extract it if it hasn't been already.
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


  /** Retrieve metadata (layer ids, env variables, volumes, ports, commands)
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

  /** Merge the layers by extracting them in order in a same directory.
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

  def generatePRootScript(scriptFile: java.io.File, config: ConfigurationData): Unit = {
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

    for {
      script <-scriptFile.toScala.newFileWriter().autoClosed // new PrintWriter(scriptFile)
    } {
      val writeln = (s: String) => script.write(s + "\n")

      writeln("#!/usr/bin/env bash\n")

      val workDir = config.WorkingDir match {
        case Some(s) if !s.isEmpty => s
        case _ => "/"
      }

      val entryPoint = config.Entrypoint match {
        case Some(list) => assembleCommandParts(list)
        case _ => ""
      }

      val cmd = config.Cmd match {
        case Some(list) => assembleCommandParts(list)
        case _ => ""
      }

      prepareVariables(List(
        workdirBashVAR + "=" + workDir,
        entryPointBashVar + "=" + entryPoint,
        cmdBashVar + "=" + cmd
      ), standardVarsFuncName, writeln)

      prepareEnvVariables(config.Env, envFuncName, writeln)

      prepareMapInfo(config.Volumes, "Data volumes", infoVolumesFuncName, writeln)
      prepareMapInfo(config.ExposedPorts, "Exposed ports", infoPortsFuncName, writeln)

      preparePRootCommand(writeln)
      preparePrintCommands(writeln)
      prepareCLI(writeln)
    }

    scriptFile.setExecutable(true)
  }

  def prepareCLI(write: String => Unit) =
    write(
      s"""if (( "$$#" < 1 )); then
         |  $printCommandsFuncName
         |elif [ "$$1" = 'info' ]; then
         |  if [ "$$2" = 'volumes' ]; then
         |    $infoVolumesFuncName
         |  elif [ "$$2" = 'ports' ]; then
         |    $infoPortsFuncName
         |  else
         |    $infoVolumesFuncName
         |    $infoPortsFuncName
         |  fi
         |elif [ "$$1" = 'run' ]; then
         |  if (( "$$#" < 3 )); then
         |    $printCommandsFuncName
         |  else
         |    $runPRootFuncName $$2 $$3 "$$4" $${@:5}
         |  fi
         |else
         |  $printCommandsFuncName
         |fi
        """.stripMargin)

  def preparePRootCommand(write: String => Unit) = {
    write(
      s"""function $runPRootFuncName {
         |  PROOT=`which $$1`
         |  $envFuncName
         |  $standardVarsFuncName
         |  ${assembleCommandParts(
              "$PROOT", // calling PRoot
              "-r $2", // setting guest rootfs
              "-w $" + workdirBashVAR, // setting working directory
              //       "$3", // user additional PRoot options
              //       "$" + entryPointBashVar,
              //       "$" + cmdBashVar,
              "${@:3}" // user inputs for the program
            )}
         |}
         |""".stripMargin)
  }

  def prepareVariables(args: List[String], functionName: String, write: String => Unit) = {
    write("function " + functionName + " {")
    for(arg <- args)
      write("\t" + addQuoteToRightSideOfEquality(arg))
    write("}\n")
  }

  def prepareEnvVariables(maybeArgs: Option[List[String]], functionName: String, write: String => Unit) = {
    write("function " + functionName + " {")
    maybeArgs match {
      case Some(args)     => {
        for(arg <- args)
          write("\texport " + addQuoteToRightSideOfEquality(arg))
      }
      case _              => write("\t:")
    }
    write("}\n")
  }

  def prepareMapInfo(maybeMap: Option[Map[String, String]], title: String, functionName: String, write: String => Unit) = {
    write("function " + functionName + " {")
    write("\techo \"" + title + ":\"")
    maybeMap match {
      case Some(map)      => {
        for((variable, _) <- map)
          write("\techo \"\t" + variable + "\"")
      }
      case _              =>
    }
    write("}\n")
  }

  def preparePrintCommands(write: String => Unit) = {
    write("function " + printCommandsFuncName + " {")
    write("\techo 'Commands:'")
    write("\techo '\tinfo <arg1>'")
    write("\techo '\t\tDisplay the data volumes or ports used the image.'")
    write("\techo '\t\t<arg1>: nothing, volumes or ports'")
    write("\techo ''")
    write("\techo '\trun <arg1> <arg2> <arg3> <args...>'")
    write("\techo '\t\tRun the image using PRoot.'")
    write("\techo '\t\t<arg1>: \tpath to the PRoot binary'")
    write("\techo '\t\t<arg2>: \tpath to the image root filesystem (rootfs)'")
    write("\techo '\t\t<arg3>: \toptions for PRoot. See PRoot manual for more info.'")
    write("\techo '\t\t\tWrap all of them with quotes (ex: \"-p 5432:5433 -p 8000:8001\").'")
    write("\techo '\t\t<args...>: \targuments for the image program'")
    write("}\n")
  }

  def assembleCommandParts(args:String*) = {
    var command = ""
    for (arg <- args)
      command += arg + " "
    command
  }

  def assembleCommandParts(args: List[String]) = {
    var command = ""
    for (arg <- args)
      command += arg + " "
    command
  }

  def addQuoteToRightSideOfEquality(s: String) = {
    s.split('=') match {
      case Array(left, right)  => left + "=\"" + right + "\""
      case _              => s
    }
  }

  def execute(
    image: BuiltPRootImage,
    command: Seq[String] = Vector.empty,
    proot: String = "proot",
    bind: Seq[(String, String)] = Vector.empty,
    workDirectory: Option[String] = None,
    environmentVariables: Seq[(String, String)] = Vector.empty) = {

    checkImageFile(image.file)

    val path = image.file.getAbsolutePath + "/"
    val rootFSPath = path + rootfsName

    val bindArgs = bind.map(b => s"-b ${b._1}:${b._2}").mkString(" ")
    val workDirectoryArgs = workDirectory.map(w => s"-w $w").mkString(" ")

    val commandArgs = if(command.isEmpty) image.command else command

    Process(
      Seq(
        path + launchScriptName,
        "run",
        proot,
        rootFSPath,
        s"$bindArgs $workDirectoryArgs --kill-on-exit --netcoop") ++ commandArgs, None, extraEnv = environmentVariables: _*) !!
  }

  def buildImage(image: SavedImage, workDirectory: File): BuiltPRootImage = {
    val rooFSPath = workDirectory.toScala / rootfsName
    val preparedImage = ImageBuilder.prepareImage(ImageBuilder.extractImage(image, rooFSPath.toJava))
    ImageBuilder.buildImage(preparedImage, rooFSPath.toJava)
    generatePRootScript((workDirectory.toScala / launchScriptName).toJava, preparedImage.configurationData)
    BuiltPRootImage(workDirectory, preparedImage.configurationData, preparedImage.command)
  }

}
