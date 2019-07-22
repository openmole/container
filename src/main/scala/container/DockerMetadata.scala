package container

/**
  * Copyright (C) 2017 Jonathan Passerat-Palmbach
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */

object DockerMetadata {

  import io.circe.{Decoder, Encoder, HCursor, Json}
  import io.circe.generic.extras.Configuration
  import io.circe.parser._
  import io.circe.generic.semiauto._


  implicit val customConfig: Configuration =
    Configuration.default.withDefaults.withDiscriminator("type")

  import java.time.LocalDateTime
  import java.time.format.DateTimeFormatter

  type DockerDate = LocalDateTime

  val dockerDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'")
  implicit val encodeDate: Encoder[DockerDate] = Encoder.encodeString.contramap[DockerDate](dockerDateFormatter.format(_))

  implicit val decodeDate: Decoder[DockerDate] = Decoder.decodeString.emap { str ⇒
    try Right(LocalDateTime.parse(str, dockerDateFormatter))
    catch {
      case e: Exception ⇒ Left("Date")
    }
  }

  /** Most examples seem to have at least created and created_by fields populated */
   case class HistoryEntry(
                           created:      String, //DockerDate
                           created_by:   String,
                           empty_layer: Option[Boolean],
                           author:      Option[String],
                           comment:     Option[String]
                         )
  implicit val HistoryEntryDecoder: Decoder[HistoryEntry] = new Decoder[HistoryEntry] {
    final def apply(cursor: HCursor): Decoder.Result[HistoryEntry] = {
      val created = cursor.get[String]("created").getOrElse("")
      val created_by = cursor.downField("container_config").downField("Cmd").as[List[String]].getOrElse(List("")).head
      val empty_layer = Some(cursor.get[Boolean]("throwaway").getOrElse(false))
      val author = Some(cursor.get[String]("author").getOrElse(""))
      val comment = Some(cursor.get[String]("comment").getOrElse(""))
      new Right(HistoryEntry(created, created_by, empty_layer, author, comment))
    }
  }
  implicit val historyEntryEncoder: Encoder[HistoryEntry] = deriveEncoder

  case class RootFS(
                     `type`:   String       = "layers",
                     diff_ids: List[String] = List.empty
                   )
  implicit val RootFSDecoder: Decoder[RootFS] = deriveDecoder
  implicit val RootFSEncoder: Encoder[RootFS] = deriveEncoder

  /**
    * All HealthCheck's fields are optional as a field can be omitted to indicate that the value should be inherited from the base layer
    */
  case class HealthCheck(
                          Test:     Option[List[String]],
                          Interval: Option[Int],
                          Timeout:  Option[Int],
                          Retries:  Option[Int]
                        )
  implicit val HealthCheckDecoder: Decoder[HealthCheck] = deriveDecoder
  implicit val HealthCheckEncoder: Encoder[HealthCheck] = deriveEncoder


  // TODO check whether these map correctly
  case class EmptyObject()
  implicit val EmptyObjectDecoder: Decoder[EmptyObject] = deriveDecoder
  implicit val EmptyObjectEncoder: Encoder[EmptyObject] = deriveEncoder

  type Volumes = Map[String, EmptyObject]
  implicit val VolumesDecoder: Decoder[Volumes] = Decoder.decodeMap[String, EmptyObject]
  implicit val VolumesEncoder: Encoder[Volumes] = Encoder.encodeMap[String, EmptyObject]

  /**
    * Container RunConfig Field Descriptions
    * "The execution parameters which should be used as a base when running a container using the image."
    */
  case class ContainerConfig(
                              // <--- documented fields  --->
                              User:         Option[String]              = Some(""),
                              Memory:       Option[Int]                 = Some(0),
                              MemorySwap:   Option[Int]                 = Some(0),
                              CpuShares:    Option[Int]                 = Some(0),
                              ExposedPorts: Option[Map[String, EmptyObject]] = None,
                              //    ExposedPorts: Ports               = Map.empty,

                              Env:          Option[List[String]]        = Some(List.empty),
                              Entrypoint:   Option[List[String]]        = Some(List.empty),
                              Cmd:          Option[List[String]]        = Some(List.empty),
                              Healthcheck:  Option[HealthCheck] = None,

                              Volumes:      Option[Volumes]             = Some(Map.empty),

                              WorkingDir:   Option[String]              = Some(""),
                              // <--- extra fields not part of the spec: implementation specific --->
                              Domainname:   Option[String]                   = Some(""), // udocker specific
                              AttachStdout: Option[Boolean]                  = None,
                              Hostname:     Option[String]                   = Some(""), // udocker specific
                              StdinOnce:    Option[Boolean]                  = None,
                              Labels:       Option[Map[String, EmptyObject]] = None, // FIXME what is this?
                              AttachStderr: Option[Boolean]                  = None,
                              OnBuild:      Option[List[String]]             = None, // FIXME what is this?
                              Tty:          Option[Boolean]                  = None,
                              OpenStdin:    Option[Boolean]                  = None,
                              Image:        Option[String]                   = None,
                              AttachStdin:  Option[Boolean]                  = None,
                              ArgsEscaped:  Option[Boolean]                  = None
                            )
  implicit val ContainerConfigDecoder: Decoder[ContainerConfig] = deriveDecoder
  implicit val ContainerConfigEncoder: Encoder[ContainerConfig] = deriveEncoder

  type ContainerID = String
  type Command = String

  /**
    * Image JSON (term from Terminology https://github.com/moby/moby/blob/master/image/spec/v1.2.md#terminology)
    *
    * Representation of the metadata stored in the json file under the key Config in manifest.json
    *
    * @see https://github.com/moby/moby/blob/master/image/spec/v1.2.md
    */

  case class ImageJSON(
                        // <--- documented fields  --->
                        created:      Option[DockerDate] = None,
                        author:       Option[String]             = Some(""),
                        architecture: Option[String]             = Some(""),
                        os:           Option[String]             = Some(""),
                        config:       Option[ContainerConfig]    = Some(ContainerConfig()),
                        rootfs:       Option[RootFS]             = Some(RootFS()),
                        history:      Option[List[HistoryEntry]] = Some(List.empty),
                        // <--- extra fields not part of the spec: implementation specific --->
                        id:               Option[String],
                        parent:           Option[String],
                        docker_version:   Option[String]          = None,
                        container:        Option[ContainerID]     = None,
                        container_config: Option[ContainerConfig] = None
                      )
  implicit val imageJSONDecoder: Decoder[ImageJSON] = deriveDecoder
  implicit val imageJSONEncoder: Encoder[ImageJSON] = deriveEncoder

  import Registry.Manifest
  def v1HistoryToImageJson(manifest: Manifest, layersHash: Map[String, String]): ImageJSON = {
    val rawJsonImage = parse(manifest.value.history.get.head.v1Compatibility).getOrElse(Json.Null)
    val cursor: HCursor = rawJsonImage.hcursor
    val created = cursor.get[DockerDate]("created").toOption
    val author = cursor.get[String]("author").toOption
    val architecture = cursor.get[String]("architecture").toOption
    val os = cursor.get[String]("os").toOption
    val config = cursor.downField("config").as[ContainerConfig].toOption //
    val rootfs = Some(RootFS(diff_ids = manifest.value.fsLayers.get.reverse.map { l => "sha256:" + layersHash(l.blobSum) } ))
    val history =
      for ( x <- manifest.value.history.get.tail.reverse) yield decode[HistoryEntry](x.v1Compatibility) match {
        case Right(value) => value
      }//)
    val id = cursor.get[String]("id").toOption
    val parent = cursor.get[String]("parent").toOption
    val docker_version = cursor.get[String]("docker_version").toOption
    val container = cursor.get[String]("container").toOption
    val container_config = cursor.get[ContainerConfig]("container_config").toOption
    ImageJSON(created, author,architecture, os, config, rootfs, Some(history), id, parent, docker_version,
      container, container_config)
  }

  case class Digest(blobSum: String)
  case class V1History(v1Compatibility: String)

  /**
    * JSON Web Key
    * @see http://self-issued.info/docs/draft-ietf-jose-json-web-signature.html#jwkDef
    */
  case class JWK(
                  crv: Option[String],
                  kid: Option[String],
                  kty: Option[String],
                  x:   Option[String],
                  y:   Option[String]
                )

  /**
    * JSON Web Signature
    * @see http://self-issued.info/docs/draft-ietf-jose-json-web-signature.html#rfc.section.4
    */
  case class JOSE(
                   jwk: Option[JWK],
                   alg: Option[String]
                 )

  /**
    * Support for signed manifests
    *
    * https://docs.docker.com/registry/spec/manifest-v2-1/#signed-manifests
    *
    */
  case class Signature(
                        header:      Option[JOSE],
                        signature:   Option[String],
                        `protected`: Option[String]
                      )

  /**
    * Registry image Manifest of an image in a repo according to Docker image spec v1
    *
    * @see https://docs.docker.com/registry/spec/manifest-v2-1/
    */
  // FIXME consider all documented fields to be present (remove option)???
  case class ImageManifestV2Schema1(
                                     // <--- documented fields  --->
                                     name:          Option[String],
                                     tag:           Option[String],
                                     architecture:  Option[String],
                                     fsLayers:      Option[List[Digest]],
                                     history:       Option[List[V1History]],
                                     schemaVersion: Option[Int],
                                     // <--- extra fields not part of the spec: implementation specific --->
                                     signatures: Option[List[Signature]] = None
                                   )

  // TODO ImageManifestV2Schema2 (ref: https://docs.docker.com/registry/spec/manifest-v2-2/) (example: https://gist.github.com/harche/6f29c6fe8479cb6334d2)

  /**
    * image JSON for the top-level image according to Docker Image spec v1.2
    *
    * NOT TO BE confused with the distribution manifest, used to push and pull images
    *
    * Usually presented in a Vector[TopLevelImageManifest] with one entry per image (current one + parent images it was derived from).
    *
    * @see https:://github.com/moby/moby/blob/master/image/spec/v1.2.md#combined-image-json--filesystem-changeset-format
    */
  case class TopLevelImageManifest(
                                    Config:   String,
                                    Layers:   Vector[String],
                                    RepoTags: Vector[String],
                                    Parent:   Option[String] = None
                                  )
}
