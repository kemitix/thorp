package net.kemitix.thorp.core

import java.nio.file.Path

import net.kemitix.thorp.domain
import net.kemitix.thorp.domain.{Config, RemoteKey}

sealed trait ConfigOption {
  def update(config: Config): Config
}

object ConfigOption {
  case class Source(path: Path) extends ConfigOption {
    override def update(config: Config): Config =
      config.copy(sources = config.sources ++ path)
  }

  case class Bucket(name: String) extends ConfigOption {
    override def update(config: Config): Config =
      if (config.bucket.name.isEmpty)
        config.copy(bucket = domain.Bucket(name))
      else
        config
  }

  case class Prefix(path: String) extends ConfigOption {
    override def update(config: Config): Config =
      if (config.prefix.key.isEmpty)
        config.copy(prefix = RemoteKey(path))
      else
        config
  }

  case class Include(pattern: String) extends ConfigOption {
    override def update(config: Config): Config =
      config.copy(filters = domain.Filter.Include(pattern) :: config.filters)
  }

  case class Exclude(pattern: String) extends ConfigOption {
    override def update(config: Config): Config =
      config.copy(filters = domain.Filter.Exclude(pattern) :: config.filters)
  }

  case class Debug() extends ConfigOption {
    override def update(config: Config): Config = config.copy(debug = true)
  }

  case object Version extends ConfigOption {
    override def update(config: Config): Config = config
  }

  case object BatchMode extends ConfigOption {
    override def update(config: Config): Config = config.copy(batchMode = true)
  }

  case object IgnoreUserOptions extends ConfigOption {
    override def update(config: Config): Config = config
  }
  case object IgnoreGlobalOptions extends ConfigOption {
    override def update(config: Config): Config = config
  }

}
