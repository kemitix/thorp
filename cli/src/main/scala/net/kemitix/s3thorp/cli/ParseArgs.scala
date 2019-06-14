package net.kemitix.s3thorp.cli

import java.io.File
import java.nio.file.Paths

import net.kemitix.s3thorp.domain.Filter.{Exclude, Include}
import net.kemitix.s3thorp.domain.{Bucket, Config, RemoteKey}
import scopt.OParser

object ParseArgs {

  val configParser: OParser[Unit, Config] = {
    val parserBuilder = OParser.builder[Config]
    import parserBuilder._
    OParser.sequence(
      programName("s3thorp"),
      head("s3thorp"),
      opt[String]('s', "source")
        .action((str, c) => c.copy(source = Paths.get(str).toFile))
        .validate(s => if (new File(s).isDirectory) Right(()) else Left("Source is not a directory"))
        .required()
        .text("Source directory to sync to S3"),
      opt[String]('b', "bucket")
        .action((str, c) => c.copy(bucket = Bucket(str)))
        .required()
        .text("S3 bucket name"),
      opt[String]('p', "prefix")
        .action((str, c) => c.copy(prefix = RemoteKey(str)))
        .text("Prefix within the S3 Bucket"),
      opt[Seq[String]]('i', "include")
        .unbounded()
        .action((str, c) => c.copy(filters = c.filters ++ str.map(Include)))
        .text("Include only matching paths"),
      opt[Seq[String]]('x', "exclude")
        .unbounded()
        .action((str,c) => c.copy(filters = c.filters ++ str.map(Exclude)))
        .text("Exclude matching paths"),
      opt[Int]('v', "verbose")
        .validate(i =>
          if (i >= 1 && i <= 5) Right(Unit)
          else Left("Verbosity level must be between 1 and 5"))
        .action((i, c) => c.copy(verbose = i))
        .text("Verbosity level (1-5)")
    )
  }

  def apply(args: List[String], defaultConfig: Config): Option[Config] =
    OParser.parse(configParser, args, defaultConfig)

}
