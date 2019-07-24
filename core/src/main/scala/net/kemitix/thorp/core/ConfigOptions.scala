package net.kemitix.thorp.core

import net.kemitix.thorp.domain.SimpleLens

case class ConfigOptions(
    options: List[ConfigOption] = List()
) {

  def combine(
      x: ConfigOptions,
      y: ConfigOptions
  ): ConfigOptions =
    x ++ y

  def ++(other: ConfigOptions): ConfigOptions =
    ConfigOptions(options ++ other.options)

  def ::(head: ConfigOption): ConfigOptions =
    ConfigOptions(head :: options)

  def contains[A1 >: ConfigOption](elem: A1): Boolean =
    options contains elem

}

object ConfigOptions {
  val options: SimpleLens[ConfigOptions, List[ConfigOption]] =
    SimpleLens[ConfigOptions, List[ConfigOption]](_.options,
                                                  c => a => c.copy(options = a))
}
