package net.kemitix.thorp.core

import java.nio.file.Path

import net.kemitix.thorp.config.LegacyConfig
import net.kemitix.thorp.config.LegacyConfig._
import net.kemitix.thorp.domain
import net.kemitix.thorp.domain.RemoteKey

sealed trait ConfigOption {
  def update(config: LegacyConfig): LegacyConfig
}

object ConfigOption {

  case class Source(path: Path) extends ConfigOption {
    override def update(config: LegacyConfig): LegacyConfig =
      sources.modify(_ ++ path)(config)
  }

  case class Bucket(name: String) extends ConfigOption {
    override def update(config: LegacyConfig): LegacyConfig =
      if (config.bucket.name.isEmpty)
        bucket.set(domain.Bucket(name))(config)
      else
        config
  }

  case class Prefix(path: String) extends ConfigOption {
    override def update(config: LegacyConfig): LegacyConfig =
      if (config.prefix.key.isEmpty)
        prefix.set(RemoteKey(path))(config)
      else
        config
  }

  case class Include(pattern: String) extends ConfigOption {
    override def update(config: LegacyConfig): LegacyConfig =
      filters.modify(domain.Filter.Include(pattern) :: _)(config)
  }

  case class Exclude(pattern: String) extends ConfigOption {
    override def update(config: LegacyConfig): LegacyConfig =
      filters.modify(domain.Filter.Exclude(pattern) :: _)(config)
  }

  case class Debug() extends ConfigOption {
    override def update(config: LegacyConfig): LegacyConfig =
      debug.set(true)(config)
  }

  case object Version extends ConfigOption {
    override def update(config: LegacyConfig): LegacyConfig = config
  }

  case object BatchMode extends ConfigOption {
    override def update(config: LegacyConfig): LegacyConfig =
      batchMode.set(true)(config)
  }

  case object IgnoreUserOptions extends ConfigOption {
    override def update(config: LegacyConfig): LegacyConfig = config
  }

  case object IgnoreGlobalOptions extends ConfigOption {
    override def update(config: LegacyConfig): LegacyConfig = config
  }

}
