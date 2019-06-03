lazy val legacyRoot = (project in file("."))
  .settings(
    name := "s3thorp",
    version := "0.1",
    scalaVersion := "2.12.8",

    // AWS SDK
    /// wraps the in-preview Java SDK V2 which is incomplete and doesn't support multi-part uploads
    libraryDependencies += "com.github.j5ik2o" %% "reactive-aws-s3-core" % "1.1.3",
    libraryDependencies += "com.github.j5ik2o" %% "reactive-aws-s3-cats" % "1.1.3",

    // AWS SDK - multi-part upload
    libraryDependencies += "com.amazonaws" % "aws-java-sdk-s3" % "1.11.563",

    // Logging
    libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
    libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.26",

    // testing
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.7" % "test",

    // recommended for cats-effects
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-language:postfixOps",
      "-language:higherKinds",
      "-Ypartial-unification")
  )

// cli -> aws-lib -> core -> aws-api (-> legacyRoot)

lazy val cli = (project in file("cli"))
  .dependsOn(`aws-lib`)
  .aggregate(legacyRoot, `aws-api`, core, `aws-lib`)
  .settings(
    libraryDependencies ++= Seq(
      // command line arguments parser
      "com.github.scopt" %% "scopt" % "4.0.0-RC2"
    )
  )

lazy val `aws-lib` = (project in file("aws-lib"))
  .dependsOn(core)

lazy val core = (project in file("core"))
  .dependsOn(`aws-api`)

lazy val `aws-api` = (project in file("aws-api"))
  .dependsOn(legacyRoot)
