package net.kemitix.thorp.cli

import java.io.File
import java.nio.file.Paths

import net.kemitix.thorp.domain.Filter.{Exclude, Include}
import net.kemitix.thorp.domain.{Bucket, Config, RemoteKey}
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
      opt[Unit]('d', "debug")
        .action((_, c) => c.copy(debug = true))
        .text("Enable debug logging")
    )
  }

  def apply(args: List[String], defaultConfig: Config): Option[Config] =
    OParser.parse(configParser, args, defaultConfig)

}
