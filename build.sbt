val applicationSettings = Seq(
  name := "s3thorp",
  version := "0.1",
  scalaVersion := "2.12.8"
)
val testDependencies = Seq(
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.7" % Test,
    "org.scalamock" %% "scalamock" % "4.1.0" % Test
  )
)
val commandLineParsing = Seq(
  libraryDependencies ++= Seq(
    "com.github.scopt" %% "scopt" % "4.0.0-RC2"
  )
)
val awsSdkDependencies = Seq(
  libraryDependencies ++= Seq(
    "com.amazonaws" % "aws-java-sdk-s3" % "1.11.568",
    // override the versions AWS uses, which is they do to preserve Java 6 compatibility
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.9",
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % "2.9.9"
  )
)
val loggingSettings = Seq(
  libraryDependencies ++= Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
    "org.slf4j" % "slf4j-log4j12" % "1.7.26",
  )
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

// cli -> aws-lib -> core -> aws-api -> domain

lazy val cli = (project in file("cli"))
  .settings(applicationSettings)
  .aggregate(`aws-lib`, core, `aws-api`, domain)
  .settings(loggingSettings)
  .settings(commandLineParsing)
  .dependsOn(`aws-lib`)

lazy val `aws-lib` = (project in file("aws-lib"))
  .settings(awsSdkDependencies)
  .settings(testDependencies)
  .dependsOn(core)

lazy val core = (project in file("core"))
  .settings(testDependencies)
  .dependsOn(`aws-api`)

lazy val `aws-api` = (project in file("aws-api"))
  .settings(catsEffectsSettings)
  .dependsOn(domain)

lazy val domain = (project in file("domain"))
  .settings(testDependencies)
