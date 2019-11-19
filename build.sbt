import sbtassembly.AssemblyPlugin.defaultShellScript

inThisBuild(List(
  organization := "net.kemitix.thorp",
  homepage := Some(url("https://github.com/kemitix/thorp")),
  licenses := List("mit" -> url("https://opensource.org/licenses/MIT")),
  developers := List(
    Developer(
      "kemitix",
      "Paul Campbell",
      "pcampbell@kemitix.net",
      url("https://github.kemitix.net")
    )
  )
))

val commonSettings = Seq(
  sonatypeProfileName := "net.kemitix",
  scalaVersion := "2.13.0",
  scalacOptions ++= Seq(
    "-Ywarn-unused:imports",
    "-Xfatal-warnings",
    "-feature",
    "-deprecation",
    "-unchecked",
    "-language:postfixOps",
    "-language:higherKinds"),
  wartremoverErrors ++= Warts.unsafe.filterNot(wart => List(
    Wart.Any,
    Wart.Nothing,
    Wart.Serializable,
    Wart.NonUnitStatements,
    Wart.StringPlusAny
  ).contains(wart)),
  test in assembly := {},
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case x => MergeStrategy.first
  }
)

val applicationSettings = Seq(
  name := "thorp",
)
val testDependencies = Seq(
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.8" % Test,
    "org.scalamock" %% "scalamock" % "4.4.0" % Test
  )
)
val commandLineParsing = Seq(
  libraryDependencies ++= Seq(
    "com.github.scopt" %% "scopt" % "4.0.0-RC2"
  )
)
val awsSdkDependencies = Seq(
  libraryDependencies ++= Seq(
    "com.amazonaws" % "aws-java-sdk-s3" % "1.11.677",
    // override the versions AWS uses, which is they do to preserve Java 6 compatibility
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.10.0",
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % "2.10.0",
    "javax.xml.bind" % "jaxb-api" % "2.3.1"
  )
)
val zioDependencies = Seq(
  libraryDependencies ++= Seq (
    "dev.zio" %% "zio" % "1.0.0-RC16",
    "dev.zio" %% "zio-streams" % "1.0.0-RC16"
  )
)

val eipDependencies = Seq(
  libraryDependencies ++= Seq(
    "net.kemitix" %% "eip-zio" % "0.3.2"
  )
)

lazy val thorp = (project in file("."))
  .settings(commonSettings)
  .aggregate(app, cli, config, console, domain, filesystem, lib, storage, `storage-aws`, uishell)

lazy val app = (project in file("app"))
  .settings(commonSettings)
  .settings(mainClass in assembly := Some("net.kemitix.thorp.Main"))
  .settings(applicationSettings)
  .settings(eipDependencies)
  .settings(Seq(
    assemblyOption in assembly := (
      assemblyOption in assembly).value
      .copy(prependShellScript =
        Some(defaultShellScript)),
    assemblyJarName in assembly := "thorp"
  ))
  .dependsOn(cli)
  .dependsOn(lib)
  .dependsOn(`storage-aws`)

lazy val cli = (project in file("cli"))
  .settings(commonSettings)
  .settings(testDependencies)
  .dependsOn(config)
  .dependsOn(filesystem % "test->test")

lazy val `storage-aws` = (project in file("storage-aws"))
  .settings(commonSettings)
  .settings(assemblyJarName in assembly := "storage-aws.jar")
  .settings(awsSdkDependencies)
  .settings(testDependencies)
  .dependsOn(storage)
  .dependsOn(filesystem % "compile->compile;test->test")
  .dependsOn(console)
  .dependsOn(lib)

lazy val lib = (project in file("lib"))
  .settings(commonSettings)
  .settings(assemblyJarName in assembly := "lib.jar")
  .settings(testDependencies)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version),
    buildInfoPackage := "thorp"
  )
  .dependsOn(storage)
  .dependsOn(console)
  .dependsOn(config)
  .dependsOn(domain % "compile->compile;test->test")
  .dependsOn(filesystem % "compile->compile;test->test")

lazy val storage = (project in file("storage"))
  .settings(commonSettings)
  .settings(zioDependencies)
  .settings(assemblyJarName in assembly := "storage.jar")
  .dependsOn(uishell)
  .dependsOn(domain)

lazy val uishell = (project in file("uishell"))
  .settings(commonSettings)
  .settings(zioDependencies)
  .settings(eipDependencies)
  .settings(assemblyJarName in assembly := "uishell.jar")
  .dependsOn(config)
  .dependsOn(console)
  .dependsOn(filesystem)

lazy val console = (project in file("console"))
  .settings(commonSettings)
  .settings(zioDependencies)
  .settings(assemblyJarName in assembly := "console.jar")
  .dependsOn(domain)

lazy val config = (project in file("config"))
  .settings(commonSettings)
  .settings(zioDependencies)
  .settings(testDependencies)
  .settings(commandLineParsing)
  .settings(assemblyJarName in assembly := "config.jar")
  .dependsOn(domain % "compile->compile;test->test")
  .dependsOn(filesystem)

lazy val filesystem = (project in file("filesystem"))
  .settings(commonSettings)
  .settings(zioDependencies)
  .settings(testDependencies)
  .settings(assemblyJarName in assembly := "filesystem.jar")
  .dependsOn(domain % "compile->compile;test->test")

lazy val domain = (project in file("domain"))
  .settings(commonSettings)
  .settings(assemblyJarName in assembly := "domain.jar")
  .settings(testDependencies)
  .settings(zioDependencies)
  .settings(eipDependencies)
