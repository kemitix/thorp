package net.kemitix.thorp.cli

import java.nio.file.Paths

import net.kemitix.thorp.core.{ConfigOption, ConfigOptions}
import scopt.OParser

object ParseArgs {

  val configParser: OParser[Unit, List[ConfigOption]] = {
    val parserBuilder = OParser.builder[List[ConfigOption]]
    import parserBuilder._
    OParser.sequence(
      programName("thorp"),
      head("thorp"),
      opt[Unit]('V', "version")
        .action((_, cos) => ConfigOption.Version :: cos)
        .text("Show version"),
      opt[Unit]('B', "batch")
        .action((_, cos) => ConfigOption.BatchMode :: cos)
        .text("Enable batch-mode"),
      opt[String]('s', "source")
        .unbounded()
        .action((str, cos) => ConfigOption.Source(Paths.get(str)) :: cos)
        .text("Source directory to sync to destination"),
      opt[String]('b', "bucket")
        .action((str, cos) => ConfigOption.Bucket(str) :: cos)
        .text("S3 bucket name"),
      opt[String]('p', "prefix")
        .action((str, cos) => ConfigOption.Prefix(str) :: cos)
        .text("Prefix within the S3 Bucket"),
      opt[String]('i', "include")
        .unbounded()
        .action((str, cos) => ConfigOption.Include(str) :: cos)
        .text("Include only matching paths"),
      opt[String]('x', "exclude")
        .unbounded()
        .action((str, cos) => ConfigOption.Exclude(str) :: cos)
        .text("Exclude matching paths"),
      opt[Unit]('d', "debug")
        .action((_, cos) => ConfigOption.Debug() :: cos)
        .text("Enable debug logging"),
      opt[Unit]("no-global")
        .action((_, cos) => ConfigOption.IgnoreGlobalOptions :: cos)
        .text("Ignore global configuration"),
      opt[Unit]("no-user")
        .action((_, cos) => ConfigOption.IgnoreUserOptions :: cos)
        .text("Ignore user configuration")
    )
  }

  def apply(args: List[String]): Option[ConfigOptions] =
    OParser
      .parse(configParser, args, List())
      .map(ConfigOptions)

}
