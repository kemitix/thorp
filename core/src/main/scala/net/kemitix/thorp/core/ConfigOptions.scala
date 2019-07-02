package net.kemitix.thorp.core

import cats.Semigroup

case class ConfigOptions(options: List[ConfigOption] = List())
    extends Semigroup[ConfigOptions] {

  override def combine(x: ConfigOptions, y: ConfigOptions): ConfigOptions =
    x ++ y

  def ++(other: ConfigOptions): ConfigOptions =
    ConfigOptions(options ++ other.options)

  def ::(head: ConfigOption): ConfigOptions =
    ConfigOptions(head :: options)

  def contains[A1 >: ConfigOption](elem: A1): Boolean =
    options contains elem

}
