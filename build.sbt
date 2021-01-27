// Copyright 2021 IBM Corp.
// SPDX-License-Identifier: Apache-2.0

name := "xskipper-regex-plugin"

crossScalaVersions := Seq("2.12.8")

scalaVersion := crossScalaVersions.value.head

sparkVersion := "3.0.1"

libraryDependencies ++= Seq (
  //  TODO: add once the jar is published
  // "io.xskipper" %% "xskipper" % "1.2.0",
  "org.apache.spark" %% "spark-hive" % sparkVersion.value % "provided",
  "org.apache.spark" %% "spark-sql" % sparkVersion.value % "provided",
  "org.apache.spark" %% "spark-core" % sparkVersion.value % "provided",
  "org.apache.spark" %% "spark-catalyst" % sparkVersion.value % "provided",

  // test dependencies
  //  TODO: add once the jar is published
  // "io.xskipper" %% "xskipper" % "1.2.0"  % "test",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "org.apache.spark" %% "spark-catalyst" % sparkVersion.value % "test",
  "org.apache.spark" %% "spark-core" % sparkVersion.value % "test",
  "org.apache.spark" %% "spark-sql" % sparkVersion.value % "test",
  "org.apache.spark" %% "spark-hive" % sparkVersion.value % "test"
)

/**
 * Test settings
 */
// Tests cannot be run in parallel since multiple Spark contexts cannot run in the same JVM.
parallelExecution in Test := false

fork in Test := true

// Configurations to speed up tests and reduce memory footprint
javaOptions in Test ++= Seq(
  "-Dspark.ui.enabled=false",
  "-Dspark.ui.showConsoleProgress=false",
  "-Xmx1024m"
)

/**
 * ScalaStyle settings
 */

scalastyleConfig := baseDirectory.value / "scalastyle-config.xml"

// Run as part of compile task
lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")
compileScalastyle := scalastyle.in(Compile).toTask("").value
(compile in Compile) := ((compile in Compile) dependsOn compileScalastyle).value

// Run as part of test task
lazy val testScalastyle = taskKey[Unit]("testScalastyle")
testScalastyle := scalastyle.in(Test).toTask("").value
(test in Test) := ((test in Test) dependsOn testScalastyle).value

/**
 * Spark Packages settings
 */

spName := "xskipper-io/xskipper-regex-plugin"

spAppendScalaVersion := true

spIncludeMaven := true

spIgnoreProvided := true

packageBin in Compile := spPackage.value

/*
 * Doc settings
 */

scalacOptions in (Compile, doc) ++= Seq(
  "-no-link-warnings" // Suppresses problems with Scaladoc @throws links
)

/**
 * Release settings
 */
organization := "io.xskipper"
organizationName := "xskipper"
organizationHomepage := Some(url("http://www.xskipper.io/"))
description := "Xskipper: An Indexing Subsystem for Apache Spark"
licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

// TODO: add release repo
publishTo := Some(Resolver.file("file", new File("/tmp/check_release")))

publishMavenStyle := true
releaseCrossBuild := true

scmInfo := Some(
  ScmInfo(
    url("https://github.com/xskipper-io/xskipper-regex-plugin"),
    "scm:git@github.com:xskipper-io/xskipper-regex-plugin.git"
  )
)

developers := List(
  Developer(
    id = "guykhazma",
    name = "Guy Khazma",
    email = "",
    url = url("https://github.com/guykhazma")
  ),
  Developer(
    id = "gallushi",
    name = "Gal Lushi",
    email = "",
    url = url("https://github.com/gallushi")
  ),
  Developer(
    id = "oshritf",
    name = "Oshrit Feder",
    email = "",
    url = url("https://github.com/oshritf")
  )
)

import ReleaseTransformations._

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  publishArtifacts,
  setNextVersion,
  commitNextVersion
)