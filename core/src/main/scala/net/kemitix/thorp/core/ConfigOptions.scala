package net.kemitix.thorp.core

import monocle.Lens
import monocle.macros.GenLens

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
  val options: Lens[ConfigOptions, List[ConfigOption]] =
    GenLens[ConfigOptions](_.options)
}
