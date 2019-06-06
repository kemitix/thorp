val testDependencies = Seq(
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.7" % "test"
  )
)
val commandLineParsing = Seq(
  libraryDependencies ++= Seq(
    "com.github.scopt" %% "scopt" % "4.0.0-RC2"
  )
)
val awsSdkDependencies = Seq(
  libraryDependencies ++= Seq(
    /// wraps the in-preview Java SDK V2 which is incomplete and doesn't support multi-part uploads
    "com.github.j5ik2o" %% "reactive-aws-s3-core" % "1.1.3",
    "com.github.j5ik2o" %% "reactive-aws-s3-cats" % "1.1.3",
    // AWS SDK - multi-part upload
    "com.amazonaws" % "aws-java-sdk-s3" % "1.11.563",
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
    "org.typelevel" %% "cats-effect" % "1.2.0"
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
lazy val legacyRoot = (project in file("."))
  .settings(
    name := "s3thorp",
    version := "0.1",
    scalaVersion := "2.12.8"
  )
  .dependsOn(`aws-lib`)

// cli (-> legacyRoot)-> aws-lib  -> core -> aws-api -> domain

lazy val cli = (project in file("cli"))
  .dependsOn(`aws-lib`)
  .aggregate(legacyRoot, `aws-api`, core, `aws-lib`, domain)
  .settings(loggingSettings)
  .settings(commandLineParsing)

lazy val `aws-lib` = (project in file("aws-lib"))
  .dependsOn(core)
  .settings(awsSdkDependencies)
  .settings(testDependencies)

lazy val core = (project in file("core"))
  .dependsOn(`aws-api`)
  .settings(testDependencies)

lazy val `aws-api` = (project in file("aws-api"))
  .dependsOn(domain)
  .settings(catsEffectsSettings)

lazy val domain = (project in file("domain"))
  .settings(testDependencies)
