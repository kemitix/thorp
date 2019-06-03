package net.kemitix.s3thorp.cli

import java.nio.file.Paths

import cats.effect.IO
import net.kemitix.s3thorp._
import net.kemitix.s3thorp.domain.{Bucket, Config, Exclude, Filter, RemoteKey}
import scopt.OParser
import scopt.OParser.{builder, parse, sequence}

object ParseArgs {

  val configParser: OParser[Unit, Config] = {
    val parserBuilder = builder[Config]
    import parserBuilder._
    sequence(
      programName("s3thorp"),
      head("s3thorp"),
      opt[String]('s', "source")
        .action((str, c) => c.copy(source = Paths.get(str).toFile))
        .required()
        .text("Source directory to sync to S3"),
      opt[String]('b', "bucket")
        .action((str, c) => c.copy(bucket = Bucket(str)))
        .required()
        .text("S3 bucket name"),
      opt[String]('p', "prefix")
        .action((str, c) => c.copy(prefix = RemoteKey(str)))
        .text("Prefix within the S3 Bucket"),
      opt[Seq[String]]('f', "filter")
      .action((str, c) => c.copy(filters = str.map(Filter)))
        .text("Filter only matching paths"),
      opt[Seq[String]]('x', "exclude")
        .action((str,c) => c.copy(excludes = str.map(Exclude)))
        .text("Exclude matching paths"),
      opt[Int]('v', "verbose")
        .validate(i =>
          if (i >= 1 && i <= 5) Right(Unit)
          else Left("Verbosity level must be between 1 and 5"))
        .action((i, c) => c.copy(verbose = i))
        .text("Verbosity level (1-5)")
    )
  }

  def apply(args: List[String], defaultConfig: Config): IO[Config] =
    parse(configParser, args, defaultConfig) match {
      case Some(config) => IO.pure(config)
      case _ => IO.raiseError(new IllegalArgumentException)
    }

}
