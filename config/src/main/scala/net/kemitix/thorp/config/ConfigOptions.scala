package net.kemitix.thorp.config

import scala.jdk.CollectionConverters._

final case class ConfigOptions(options: java.util.List[ConfigOption]) {

  def ++(other: ConfigOptions): ConfigOptions =
    ConfigOptions.combine(this, other)

  def ::(head: ConfigOption): ConfigOptions =
    ConfigOptions((head :: options.asScala.toList).asJava)

}

object ConfigOptions {
  val defaultParallel = 1
  def parallel(configOptions: ConfigOptions): Int = {
    configOptions.options.asScala
      .collectFirst {
        case ConfigOption.Parallel(factor) => factor
      }
      .getOrElse(defaultParallel)
  }

  val empty: ConfigOptions = ConfigOptions(List.empty.asJava)
  val options: SimpleLens[ConfigOptions, List[ConfigOption]] =
    SimpleLens[ConfigOptions, List[ConfigOption]](
      _.options.asScala.toList,
      c => a => c.copy(options = a.asJava))
  def combine(
      x: ConfigOptions,
      y: ConfigOptions
  ): ConfigOptions =
    ConfigOptions((x.options.asScala.toList ++ y.options.asScala.toList).asJava)

  def contains[A1 >: ConfigOption](elem: A1)(
      configOptions: ConfigOptions): Boolean =
    configOptions.options.contains(elem)
}
