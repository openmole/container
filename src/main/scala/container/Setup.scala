package container

import squants.time._
import scala.reflect.io.File

case class Setup(
  directoryPath: File,
  binPath: File,
  networkService: NetworkService,
  timeout: Time)
