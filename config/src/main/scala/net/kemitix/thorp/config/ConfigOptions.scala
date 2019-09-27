package net.kemitix.thorp.config

import net.kemitix.thorp.domain.SimpleLens

final case class ConfigOptions(options: List[ConfigOption]) {

  def ++(other: ConfigOptions): ConfigOptions =
    ConfigOptions.combine(this, other)

  def ::(head: ConfigOption): ConfigOptions =
    ConfigOptions(head :: options)

}

object ConfigOptions {
  val defaultParallel = 1
  def parallel(configOptions: ConfigOptions): Int = {
    configOptions.options
      .collectFirst {
        case ConfigOption.Parallel(factor) => factor
      }
      .getOrElse(defaultParallel)
  }

  val empty: ConfigOptions = ConfigOptions(List.empty)
  val options: SimpleLens[ConfigOptions, List[ConfigOption]] =
    SimpleLens[ConfigOptions, List[ConfigOption]](_.options,
                                                  c => a => c.copy(options = a))
  def combine(
      x: ConfigOptions,
      y: ConfigOptions
  ): ConfigOptions = ConfigOptions(x.options ++ y.options)

  def contains[A1 >: ConfigOption](elem: A1)(
      configOptions: ConfigOptions): Boolean =
    configOptions.options.contains(elem)
}
