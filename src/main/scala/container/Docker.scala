package container

import java.io.File
import java.util.UUID

import container.ImageBuilder.checkImageFile
import container.tool.Tar

import scala.sys.process._

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
object Docker {

  case class BuiltDockerImage(
    file: File,
    imageId: String,
    //   configurationData: ConfigurationData,
    command: Seq[String] = Seq())

  def build(image: SavedImage, archive: File, dockerCommand: String = "docker"): BuiltDockerImage = {
    //if (image.compressed) BuiltDockerImage(image.file, image.imageName, image.command)
    //val path = image.file.toScala.pathAsString
    //BFile(path + "/manifest.json").delete()
    Tar.archive(image.file, archive)
    checkImageFile(image.file)

    val imageId = UUID.randomUUID().toString

    val file = archive.getAbsolutePath

    val out = (s"$dockerCommand load -i $file").!!
    val name = out.split("\n").head.split(":").drop(1).mkString(":").trim
    (s"$dockerCommand tag ${name} $imageId").!!

    BuiltDockerImage(archive, imageId)
  }

  def execute(image: BuiltDockerImage, command: Option[Seq[String]] = None, dockerCommand: String = "docker") =
    Seq(dockerCommand, "run", "--rm", "--name", image.imageId, image.imageId) ++ command.getOrElse(image.command) !!

  def executeFlatImage(
    image: FlatImage,
    tmpDirectory: File,
    command: Seq[String] = Seq.empty,
    dockerCommand: String = "docker",
    bind: Seq[(String, String)] = Vector.empty,
    workDirectory: Option[String] = None,
    environmentVariables: Seq[(String, String)] = Vector.empty,
    logger: ProcessLogger = tool.outputLogger) = {
    import better.files._

    val id = UUID.randomUUID().toString

    val buildDirectory = tmpDirectory.toScala / id
    buildDirectory.createDirectoryIfNotExists(createParents = true)

     (buildDirectory / ".empty").createFile()

    (buildDirectory / "Dockerfile").writeText(
      """
        |FROM scratch
        |COPY ./.empty /
        |""".stripMargin)

    Seq("docker", "build", "-t", id, buildDirectory.toJava.getAbsolutePath) !!

    def variables =
      image.env.getOrElse(Seq.empty).flatMap { e =>
        val name = e.takeWhile(_ != '=')
        val value = e.dropWhile(_ != '=').drop(1)
        Seq("-e", s"""$name:"$value" """)
      } ++ environmentVariables.flatMap { e =>
        Seq("-e", s"""${e._1}:"${e._2}" """)
      }

    def volumes =
      (image.file.toScala / FlatImage.rootfsName).list.filter(f => !Set("proc", "dev", "run").contains(f.name)).flatMap {
        f => Seq("-v", s"${f.toJava.getAbsolutePath}:/${f.toJava.getName}")
      } ++ bind.flatMap { b => Seq("-v", s"""${b._1}:"${b._2}" """) }

    val workDirectoryValue = workDirectory.orElse(image.workDirectory).map(w => Seq("-w", w)).getOrElse(Seq.empty)
    val cmd = if(!command.isEmpty)  command else image.command

    val run =
      Seq(
        dockerCommand,
        "run",
        "--user",
        "0",
        "--rm",
        "--name",
        id,
      ) ++ workDirectoryValue ++ volumes ++ variables ++ Seq(id) ++ cmd

    try run ! logger
    finally Seq("docker", "rmi", id) !!
  }


  def clean(image: BuiltDockerImage, dockerCommand: String = "docker") = {
    (s"$dockerCommand rmi ${image.imageId}").!!
    image.file.delete()
  }

}
