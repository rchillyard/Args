organization := "com.phasmidsoftware"

name := "Args"

version := "1.0.0"

scalaVersion := "2.12.5"

crossScalaVersions := Seq("2.10.6","2.11.8","2.12.5")

val scalaModules = "org.scala-lang.modules"
val scalaParser = "scala-parser-combinators"

val scalaTestGroup = "org.scalatest"
val scalaTestArt = "scalatest"

lazy val scalaParserVersion = "1.0.6"
lazy val scalaTestVersion = SettingKey[String]("scalaTestVersion")

scalaTestVersion := (scalaBinaryVersion.value match {
  case "2.10" => "2.2.6"
  case "2.11" => "3.0.1"
  case "2.12" => "3.0.5"
})

libraryDependencies ++= (scalaBinaryVersion.value match {
  case "2.12" =>   Seq(
    scalaModules %% scalaParser % scalaParserVersion
  )
  case "2.11" =>   Seq(
    scalaModules %% scalaParser % scalaParserVersion
  )
  case _ => Seq()
})

libraryDependencies ++= Seq(
  scalaTestGroup %% scalaTestArt % scalaTestVersion.value % "test"
)

parallelExecution in Test := false
