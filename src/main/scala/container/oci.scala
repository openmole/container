/*
 * Copyright (C) 2016 Vincent Hage
 * Copyright (C) 2019 Pierre Peigne
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
package container

import container.Status._
import java.io.{ File, IOException }
import java.nio.file._

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import io.circe.parser._
import io.circe.Decoder
import io.circe.generic.semiauto._

object InternetProtocol {
  sealed abstract class InternetProtocol(val typeProtocol: String)
  case object TCP extends InternetProtocol("TCP")
  case object UDP extends InternetProtocol("UDP")
  case object OTHER extends InternetProtocol("OTHER")
}

object OCI {
  case class InvalidManifest(manifest: String) extends Exception
  case class InvalidConfigData(config: String) extends Exception
  case class EmptyObject()

  case class ManifestData(
    Config: Option[String],
    RepoTags: List[String],
    Layers: List[String])

  implicit val ManifestDecoder: Decoder[ManifestData] = deriveDecoder

  case class ConfigurationData(
    Cmd: Option[List[String]],
    Entrypoint: Option[List[String]],
    Env: Option[List[String]],
    ExposedPorts: Option[Map[String, String]], //EmptyObject]],
    User: Option[String],
    Volumes: Option[Map[String, String]], //EmptyObject]],
    WorkingDir: Option[String])
  implicit val ConfigurationDataDecoder: Decoder[ConfigurationData] = deriveDecoder

  //TODO: test
  def getManifestFile(rootFiles: Array[String]): Option[String] = {
    val manifests = rootFiles.filter(_.endsWith("manifest.json"))
    if (!manifests.isEmpty) Some(manifests(0))
    else None
  }

  def harvestManifestData(manifestRaw: String) = {
    val manifest = decode[List[ManifestData]](manifestRaw)

    manifest match {
      case Right(m) => m.head
      case Left(error) => throw error
    }
  }

  def harvestConfigData(configRaw: String) = {
    case class ConfigFile(config: Option[ConfigurationData])
    implicit val ConfigurationFileDecoder: Decoder[ConfigFile] = deriveDecoder

    val config = decode[ConfigFile](configRaw)

    config.toOption.flatMap(_.config) match {
      case Some(c) => c
      case _ => throw InvalidConfigData(configRaw)
    }
  }

  object WhiteoutUtils {
    /* @see <a href="https://github.com/docker/docker/blob/master/pkg/archive/whiteouts.go">Whiteout files</a> */
    /* indicates a file removed in recent layers */
    val whiteoutPrefix = ".wh."
    /* indicates special file, generally excluded from exported archives */
    val whiteoutMetaPrefix = whiteoutPrefix + whiteoutPrefix
    /* indicates a link removed in recent layers */
    val whiteoutLinkDir = whiteoutPrefix + "plnk"
    /* indicates that the current directory is inaccessible in recent layers
        * (the directory file should have a whiteout file in the parent folder) */
    val whiteoutOpaqueDir = whiteoutPrefix + ".opq."

    def getPrefix(path: Path) = {
      var filename = path.getFileName.normalize().toString()

      if (filename.startsWith(whiteoutOpaqueDir)) whiteoutOpaqueDir
      else if (filename.startsWith(whiteoutLinkDir)) whiteoutLinkDir
      else if (filename.startsWith(whiteoutMetaPrefix)) whiteoutMetaPrefix
      else if (filename.startsWith(whiteoutPrefix)) whiteoutPrefix
      else ""
    }
  }
  import WhiteoutUtils._

  def removeWhiteouts(directory: File): Status = {
    var whiteoutsBuffer = new ListBuffer[Path]()

    Files.walk(directory.toPath).iterator().asScala
      .filter(_.getFileName.normalize().toString().startsWith(whiteoutPrefix))
      .foreach(
        whiteoutPath => {
          whiteoutsBuffer += whiteoutPath

          var prefix = getPrefix(whiteoutPath)
          getConcernedFile(whiteoutPath, prefix) match {
            case Some(path) => whiteoutsBuffer += path
            case None =>
          }
        })

    val whiteouts = whiteoutsBuffer.toList
    whiteouts.foreach(path => deleteRecursively(path))

    return OK
  }

  def getConcernedFile(whiteoutFilePath: Path, prefix: String) = {
    val tempStr = whiteoutFilePath.normalize().toString()
    val filePathString = tempStr.replaceAll(prefix, "")
    val filePath = Paths.get(filePathString)

    Files.exists(filePath) match {
      case true => Some(filePath)
      case false => None
    }
  }

  def removeWhiteoutFileAuxiliary(whiteoutFilePath: Path, prefix: String): Status = {
    val pathString = whiteoutFilePath.normalize().toString()
    val correspondingFile = pathString.replaceAll(prefix, "")

    try {
      deleteRecursively(Paths.get(correspondingFile))
      deleteRecursively(Paths.get(pathString))
    } catch {
      case se: SecurityException => return SECURITY_ERROR.withArgument(se)
      case ioe: IOException => return IO_ERROR.withArgument(ioe)
      case dnee: DirectoryNotEmptyException => return IO_ERROR.withArgument(dnee)
      case e: Exception => return UNKNOWN_ERROR.withArgument(e)
    }
    return OK
  }

  def deleteRecursively(path: Path): Unit =
    if (Files.isDirectory(path)) {
      Files.walk(path).iterator().asScala.foreach(p => if (!p.equals(path)) deleteRecursively(p))
      Files.deleteIfExists(path)
    } else Files.deleteIfExists(path)

}

