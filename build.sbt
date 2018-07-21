organization := "com.phasmid"

name := "Args"

version := "1.0.0"

scalaVersion := "2.12.5"

crossScalaVersions := Seq("2.10.6","2.11.8","2.12.5")

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

val scalaModules = "org.scala-lang.modules"

val scalaTestGroup = "org.scalatest"
val scalaTestArt = "scalatest"

val scalaCheckGroup = "org.scalacheck"
val scalaCheckArt = "scalacheck"

val typesafeGroup = "com.typesafe"
val configVersion = "1.3.1"

lazy val scalaParserVersion = "1.0.6"
lazy val scalaTestVersion = SettingKey[String]("scalaTestVersion")
lazy val scalaCheckVersion = SettingKey[String]("scalaCheckVersion")

scalaTestVersion := (scalaBinaryVersion.value match {
  case "2.10" => "2.2.6"
  case "2.11" => "3.0.1"
  case "2.12" => "3.0.5"
})
scalaCheckVersion := (scalaBinaryVersion.value match {
  case "2.10" => "1.12.6"
  case "2.11" => "1.12.6"
  case "2.12" => "1.13.5"
})

libraryDependencies ++= (scalaBinaryVersion.value match {
  case "2.12" =>   Seq(
    scalaModules %% "scala-parser-combinators" % scalaParserVersion,
    // NOTE: we don't need this but dependencies apparently use different versions:
    scalaModules %% "scala-xml" % "1.0.6",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.7.0"
  )
  case "2.11" =>   Seq(
    scalaModules %% "scala-parser-combinators" % scalaParserVersion,
    // NOTE: we don't need this but dependencies apparently use different versions:
    scalaModules %% "scala-xml" % scalaParserVersion,
    // NOTE: only used for testing
    "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0"
  )
  case "2.10" =>   Seq(
    "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2"
  )
  case _ => Seq()
})

libraryDependencies ++= Seq(
  typesafeGroup % "config" % configVersion,
  "ch.qos.logback" %  "logback-classic" % "1.1.7" % "runtime",
  scalaCheckGroup %% scalaCheckArt % scalaCheckVersion.value % "test",
  scalaTestGroup %% scalaTestArt % scalaTestVersion.value % "test"
)

//libraryDependencies ++= Seq(
//  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
//  scalaModules %% "scala-parser-combinators" % scalaParserVersion
//)

parallelExecution in Test := false
