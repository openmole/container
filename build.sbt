
scalaVersion := "2.12.10"
name := "container"
organization := "org.openmole"

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

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)

//libraryDependencies += "org.scalatest" % "scalatest_2.12" % "3.0.5" % "test"


/* Publish */

publishMavenStyle in ThisBuild := true
publishArtifact in Test in ThisBuild := false
publishArtifact := false
pomIncludeRepository in ThisBuild := { _ => false }

publishTo in ThisBuild := sonatypePublishToBundle.value

pomIncludeRepository in ThisBuild := { _ => false }

licenses in ThisBuild := Seq("Affero GPLv3" -> url("http://www.gnu.org/licenses/"))

homepage in ThisBuild := Some(url("https://github.com/openmole/container"))

scmInfo in ThisBuild := Some(ScmInfo(url("https://github.com/openmole/container.git"), "scm:git:git@github.com:openmole/container.git"))

pomExtra in ThisBuild := (
  <developers>
    <developer>
      <id>romainreuillon</id>
      <name>Romain Reuillon</name>
    </developer>
  </developers>
)

releasePublishArtifactsAction := PgpKeys.publishSigned.value

releaseVersionBump := sbtrelease.Version.Bump.Minor

releaseTagComment := s"Releasing ${(version in ThisBuild).value}"

releaseCommitMessage := s"Bump version to ${(version in ThisBuild).value}"

sonatypeProfileName := "fr.iscpif"

publishConfiguration := publishConfiguration.value.withOverwrite(true)

import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._


releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  tagRelease,
  releaseStepCommand("publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)




