val commonSettings = Seq(
  version := "DEV-SNAPSHOT",
  organization := "net.kemitix",
  scalaVersion := "2.12.8",
  test in assembly := {}
)

val applicationSettings = Seq(
  name := "thorp",
)
val testDependencies = Seq(
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.8" % Test,
    "org.scalamock" %% "scalamock" % "4.2.0" % Test
  )
)
val commandLineParsing = Seq(
  libraryDependencies ++= Seq(
    "com.github.scopt" %% "scopt" % "4.0.0-RC2"
  )
)
val awsSdkDependencies = Seq(
  libraryDependencies ++= Seq(
    "com.amazonaws" % "aws-java-sdk-s3" % "1.11.574",
    // override the versions AWS uses, which is they do to preserve Java 6 compatibility
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.9",
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % "2.9.9"
  )
)
val catsSettings = Seq (
  libraryDependencies ++=  Seq(
    "org.typelevel" %% "cats-core" % "1.6.1"
  ),
  // recommended for cats-effects
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-unchecked",
    "-language:postfixOps",
    "-language:higherKinds",
    "-Ypartial-unification")
)
val catsEffectsSettings = Seq(
  libraryDependencies ++=  Seq(
    "org.typelevel" %% "cats-effect" % "1.3.1"
  ),
  // recommended for cats-effects
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-unchecked",
    "-language:postfixOps",
    "-language:higherKinds",
    "-Ypartial-unification")
)

// cli -> thorp-lib -> aws-lib -> core -> aws-api -> domain

lazy val cli = (project in file("cli"))
  .settings(commonSettings)
  .settings(mainClass in assembly := Some("net.kemitix.thorp.cli.Main"))
  .settings(applicationSettings)
  .settings(catsEffectsSettings)
  .aggregate(`thorp-lib`, `aws-lib`, core, `aws-api`, domain)
  .settings(commandLineParsing)
  .settings(testDependencies)
  .dependsOn(`thorp-lib`)

lazy val `thorp-lib` = (project in file("thorp-lib"))
  .settings(commonSettings)
  .settings(assemblyJarName in assembly := "thorp-lib.jar")
  .dependsOn(`aws-lib`)

lazy val `aws-lib` = (project in file("aws-lib"))
  .settings(commonSettings)
  .settings(assemblyJarName in assembly := "aws-lib.jar")
  .settings(awsSdkDependencies)
  .settings(testDependencies)
  .dependsOn(core)

lazy val core = (project in file("core"))
  .settings(commonSettings)
  .settings(assemblyJarName in assembly := "core.jar")
  .settings(testDependencies)
  .dependsOn(`aws-api`)

lazy val `aws-api` = (project in file("aws-api"))
  .settings(commonSettings)
  .settings(assemblyJarName in assembly := "aws-api.jar")
  .settings(catsSettings)
  .dependsOn(domain)

lazy val domain = (project in file("domain"))
  .settings(commonSettings)
  .settings(assemblyJarName in assembly := "domain.jar")
  .settings(testDependencies)
