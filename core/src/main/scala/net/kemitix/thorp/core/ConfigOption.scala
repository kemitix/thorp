package net.kemitix.thorp.core

import java.nio.file.Path

sealed trait ConfigOption

object ConfigOption {
  case class Source(path: Path) extends ConfigOption
  case class Bucket(name: String) extends ConfigOption
  case class Prefix(path: String) extends ConfigOption
  case class Include(pattern: String) extends ConfigOption
  case class Exclude(pattern: String) extends ConfigOption
  case class Debug() extends ConfigOption
}
