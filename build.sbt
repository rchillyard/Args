organization := "com.phasmidsoftware"

name := "Args"

version := "2.1.0"

scalaVersion := "3.9.0" // the current LTS

// The compiler options that all the Phasmid libraries share.
scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Wunused:all",
  "-Wvalue-discard",
  "-Wnonunit-statement"
)
// Tests may use a value as a statement (for example, in a matcher), so they skip -Wnonunit-statement.
Test / scalacOptions ~= (_.filterNot(_ == "-Wnonunit-statement"))

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "2.4.0",
  "com.novocode" % "junit-interface" % "0.11" % "test",
  "org.scalatest" %% "scalatest" % "3.2.19" % "test"
)


// Scaladoc is published to https://rchillyard.github.io/Args/latest/api/ by `sbt ghpagesPushSite`
enablePlugins(GhpagesPlugin, SiteScaladocPlugin)
git.remoteRepo := "git@github.com:rchillyard/Args.git"
