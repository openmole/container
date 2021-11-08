
scalaVersion := "3.1.0"
crossScalaVersions := Seq("3.1.0")
name := "container"
organization := "org.openmole"

libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.5.10"
libraryDependencies += "org.typelevel"  %% "squants"  % "1.8.3" cross(CrossVersion.for2_13Use3)

libraryDependencies += "com.github.pathikrit" %% "better-files" % "3.9.1" cross(CrossVersion.for3Use2_13)
libraryDependencies += "org.apache.commons" % "commons-compress" % "1.19"
libraryDependencies += "org.apache.commons" % "commons-exec" % "1.3"

val circeVersion = "0.14.1"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  //"io.circe" %% "circe-generic-extras"
).map(_ % circeVersion)

//libraryDependencies += "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.3"

scalacOptions ++= { 
  scalaBinaryVersion.value match {
    case _ => Seq("-target:11", "-language:postfixOps", "-Ymacro-annotations", "-language:implicitConversions", "-Ytasty-reader")
  }
}

/* Publish */

publishMavenStyle in ThisBuild := true
publishArtifact in Test in ThisBuild := false
//publishArtifact := false
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

sonatypeProfileName := "org.openmole"

publishConfiguration := publishConfiguration.value.withOverwrite(true)

releaseCrossBuild := true

import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._


releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("+publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)




