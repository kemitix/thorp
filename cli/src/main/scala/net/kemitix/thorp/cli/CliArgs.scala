package net.kemitix.thorp.cli

import java.nio.file.Paths

import scala.jdk.CollectionConverters._

import net.kemitix.thorp.config.{ConfigOption, ConfigOptions}
import scopt.OParser
import zio.Task

object CliArgs {

  def parse(args: List[String]): Task[ConfigOptions] = Task {
    OParser
      .parse(configParser, args, List())
      .map(options => ConfigOptions(options.asJava))
      .getOrElse(ConfigOptions.empty)
  }

  val configParser: OParser[Unit, List[ConfigOption]] = {
    val parserBuilder = OParser.builder[List[ConfigOption]]
    import parserBuilder._
    OParser.sequence(
      programName("thorp"),
      head("thorp"),
      opt[Unit]('V', "version")
        .action((_, cos) => ConfigOption.version() :: cos)
        .text("Show version"),
      opt[Unit]('B', "batch")
        .action((_, cos) => ConfigOption.batchMode() :: cos)
        .text("Enable batch-mode"),
      opt[String]('s', "source")
        .unbounded()
        .action((str, cos) => ConfigOption.source(Paths.get(str)) :: cos)
        .text("Source directory to sync to destination"),
      opt[String]('b', "bucket")
        .action((str, cos) => ConfigOption.bucket(str) :: cos)
        .text("S3 bucket name"),
      opt[String]('p', "prefix")
        .action((str, cos) => ConfigOption.prefix(str) :: cos)
        .text("Prefix within the S3 Bucket"),
      opt[Int]('P', "parallel")
        .action((int, cos) => ConfigOption.parallel(int) :: cos)
        .text("Maximum Parallel uploads"),
      opt[String]('i', "include")
        .unbounded()
        .action((str, cos) => ConfigOption.include(str) :: cos)
        .text("Include only matching paths"),
      opt[String]('x', "exclude")
        .unbounded()
        .action((str, cos) => ConfigOption.exclude(str) :: cos)
        .text("Exclude matching paths"),
      opt[Unit]('d', "debug")
        .action((_, cos) => ConfigOption.debug() :: cos)
        .text("Enable debug logging"),
      opt[Unit]("no-global")
        .action((_, cos) => ConfigOption.ignoreGlobalOptions() :: cos)
        .text("Ignore global configuration"),
      opt[Unit]("no-user")
        .action((_, cos) => ConfigOption.ignoreUserOptions() :: cos)
        .text("Ignore user configuration")
    )
  }

}
