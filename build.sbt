// SPDX-FileCopyrightText: 2025 aesc silicon
//
// SPDX-License-Identifier: CERN-OHL-W-2.0

lazy val root = (project in file("."))
  .settings(
    name := "ElemRV",
    inThisBuild(
      List(
        organization := "com.github.spinalhdl",
        scalaVersion := "2.12.18",
        version := "1.0.0"
      )
    ),
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.5",
      "org.yaml" % "snakeyaml" % "1.8"
    ),
    Compile / scalaSource := baseDirectory.value / "hardware" / "scala",
    Test / scalaSource := baseDirectory.value / "test" / "scala",
    Test / parallelExecution := false,
    scalacOptions += s"-Xplugin:${(spinalHdlIdslPlugin / Compile / packageBin).value.getAbsolutePath}",
    scalacOptions += "-Xplugin-require:idsl-plugin",
    envVars += ("NAFARR_BASE" -> (nafarr / baseDirectory).value.getAbsolutePath)
  )
  .dependsOn(zibal, spinalHdlIdslPlugin, spinalHdlCore, spinalHdlLib, spinalHdlSim)

val zibalPath = sys.env.getOrElse("ZIBAL_PATH", "modules/elements/zibal")
val nafarrPath = sys.env.getOrElse("NAFARR_PATH", zibalPath + "/ext/nafarr")
val spinalHdlPath = sys.env.getOrElse("SPINALHDL_PATH", nafarrPath + "/ext/VexiiRiscv/ext/SpinalHDL")

lazy val zibal = RootProject(file(zibalPath))
lazy val nafarr = RootProject(file(nafarrPath))

lazy val spinalHdlIdslPlugin = ProjectRef(file(spinalHdlPath), "idslplugin")
lazy val spinalHdlCore = ProjectRef(file(spinalHdlPath), "core")
lazy val spinalHdlLib = ProjectRef(file(spinalHdlPath), "lib")
lazy val spinalHdlSim = ProjectRef(file(spinalHdlPath), "sim")

run / connectInput := true
fork := true
