organization := "com.phasmidsoftware"

name := "Args"

version := "1.0.3"

scalaVersion := "2.13.7"

crossScalaVersions := Seq("2.10.7","2.11.12","2.12.11","2.13.6")

scalacOptions += "-deprecation"

val scalaModules = "org.scala-lang.modules"
val scalaParser = "scala-parser-combinators"

val scalaTestGroup = "org.scalatest"
val scalaTestArt = "scalatest"

lazy val scalaParserVersion = "1.1.2"
lazy val scalaTestVersion = SettingKey[String]("scalaTestVersion")

scalaTestVersion := (scalaBinaryVersion.value match {
  case "2.10" => "2.2.6"
  case "2.11" => "3.0.1"
  case "2.12" => "3.0.5"
  case "2.13" => "3.2.9"
})

libraryDependencies ++= (scalaBinaryVersion.value match {
  case "2.13" =>   Seq(
    scalaModules %% scalaParser % scalaParserVersion
  )
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
