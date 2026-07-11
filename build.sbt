organization := "com.phasmidsoftware"

name := "Args"

version := "2.0.0"

scalaVersion := "3.3.8"

scalacOptions ++= Seq("-deprecation", "-feature")

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "2.4.0",
  "com.novocode" % "junit-interface" % "0.11" % "test",
  "org.scalatest" %% "scalatest" % "3.2.19" % "test"
)
