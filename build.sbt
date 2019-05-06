name := "s3thorp"

version := "0.1"

scalaVersion := "2.12.8"

// command line arguments parser
libraryDependencies += "com.github.scopt" %% "scopt" % "4.0.0-RC2"

// i/o stream processor
libraryDependencies += "co.fs2" %% "fs2-core" % "1.0.4"
libraryDependencies += "co.fs2" %% "fs2-io" % "1.0.4"

// testing
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.7" % "test"

// recommended for cats-effects
scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-language:postfixOps",
  "-language:higherKinds",
  "-Ypartial-unification")

