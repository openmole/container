
scalaVersion := "2.12.7"
name := "container"
organization := "org.openmole"

libraryDependencies += "org.typelevel" %% "cats-core" % "1.1.0"
libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.5.6"
libraryDependencies += "org.typelevel"  %% "squants"  % "1.3.0"
libraryDependencies += "org.typelevel" %% "cats-core" % "1.4.0"

libraryDependencies += "com.github.pathikrit" %% "better-files" % "3.8.0"

libraryDependencies += "org.apache.commons" % "commons-compress" % "1.18"

val circeVersion = "0.9.3"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-generic-extras"

).map(_ % circeVersion)

addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
)

libraryDependencies += "org.scalatest" % "scalatest_2.12" % "3.0.5" % "test"

