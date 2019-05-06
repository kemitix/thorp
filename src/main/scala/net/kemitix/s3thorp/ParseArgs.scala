package net.kemitix.s3thorp

import cats.effect.IO

object ParseArgs {

  def apply(args: List[String]): IO[Config] = {
    import scopt.OParser
    val builder = OParser.builder[Config]
    val configParser: OParser[Unit, Config] = {
      import builder._
      OParser.sequence(
        programName("S3Thorp"),
        head("s3thorp", "0.1.0"),
        opt[String]('s', "source")
          .action((str, c) => c.copy(source = str)),
        opt[String]('b', "bucket")
          .action((str, c) => c.copy(bucket = str))
          .text("S3 bucket name"),
        opt[String]('p', "prefix")
          .action((str, c) => c.copy(prefix = str))
          .text("Prefix within the S3 Bucket")
      )
    }
    val defaultConfig = Config("def-bucket", "def-prefix", "def-source")
    OParser.parse(configParser, args, defaultConfig) match {
      case Some(config) => IO.pure(config)
      case _ => IO.raiseError(new IllegalArgumentException)
    }
  }

}
