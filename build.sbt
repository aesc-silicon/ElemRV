val spinalVersion = "1.10.2a"

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
      "com.github.spinalhdl" % "spinalhdl-core_2.12" % spinalVersion,
      "com.github.spinalhdl" % "spinalhdl-lib_2.12" % spinalVersion,
      compilerPlugin("com.github.spinalhdl" % "spinalhdl-idsl-plugin_2.12" % spinalVersion),
      "org.scalatest" %% "scalatest" % "3.2.5",
      "org.yaml" % "snakeyaml" % "1.8"
    ),
    Compile / scalaSource := baseDirectory.value / "hardware" / "scala",
    Test / scalaSource := baseDirectory.value / "test" / "scala"
  )
  .dependsOn(nafarr, zibal)

lazy val nafarr = RootProject(file("modules/elements/nafarr/"))
lazy val zibal = RootProject(file("modules/elements/zibal/"))

run / connectInput := true
fork := true
