// SPDX-FileCopyrightText: 2025 aesc silicon
//
// SPDX-License-Identifier: Apache-2.0

// Standalone SBT sub-project for the Digital Twin Verilog generators.
//
// Invoke from this directory:
//   cd digital-twin && sbt "runMain elemrv_n.test.WishboneGpioNVerilog"
//
// The DT generators in this slice only depend on nafarr + SpinalHDL;
// they do not import anything from the upstream `hardware/scala/`
// tree nor from zibal, so this project stays isolated from the
// upstream root build.

val spinalVersion = "1.13.0"

lazy val root = (project in file("."))
  .settings(
    name := "ElemRV-DigitalTwin",
    inThisBuild(
      List(
        organization := "com.github.spinalhdl",
        scalaVersion := "2.12.18",
        version := "1.0.0"
      )
    ),
    libraryDependencies ++= Seq(
      "com.github.spinalhdl" % "spinalhdl-core_2.12" % spinalVersion,
      "com.github.spinalhdl" % "spinalhdl-lib_2.12" % spinalVersion,
      compilerPlugin("com.github.spinalhdl" % "spinalhdl-idsl-plugin_2.12" % spinalVersion)
    ),
    Compile / scalaSource := baseDirectory.value / "scala"
  )
  .dependsOn(nafarr)

lazy val nafarr = RootProject(file("../modules/elements/nafarr/"))

run / connectInput := true
fork := true
