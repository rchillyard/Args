organization := "com.phasmidsoftware"

name := "Args"

version := "2.0.0"

scalaVersion := "3.9.0" // the current LTS

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Wunused:all")

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "2.4.0",
  "com.novocode" % "junit-interface" % "0.11" % "test",
  "org.scalatest" %% "scalatest" % "3.2.19" % "test"
)

// JUnit-style reports, collected by CircleCI
Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports")

// Scaladoc is published to https://rchillyard.github.io/Args/latest/api/ by `sbt ghpagesPushSite`
enablePlugins(GhpagesPlugin, SiteScaladocPlugin)
git.remoteRepo := "git@github.com:rchillyard/Args.git"
