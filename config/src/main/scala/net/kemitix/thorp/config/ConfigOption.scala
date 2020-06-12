package net.kemitix.thorp.config

import java.nio.file.Path

import net.kemitix.thorp.config.Configuration._
import net.kemitix.thorp.domain
import net.kemitix.thorp.domain.RemoteKey

sealed trait ConfigOption {
  def update(config: Configuration): Configuration
}

object ConfigOption {

  final case class Source(path: Path) extends ConfigOption {
    override def update(config: Configuration): Configuration =
      sources.modify(_ + path)(config)
  }

  final case class Bucket(name: String) extends ConfigOption {
    override def update(config: Configuration): Configuration =
      if (config.bucket.name.isEmpty)
        bucket.set(domain.Bucket.named(name))(config)
      else
        config
  }

  final case class Prefix(path: String) extends ConfigOption {
    override def update(config: Configuration): Configuration =
      if (config.prefix.key.isEmpty)
        prefix.set(RemoteKey.create(path))(config)
      else
        config
  }

  final case class Include(pattern: String) extends ConfigOption {
    override def update(config: Configuration): Configuration =
      filters.modify(domain.Filter.include(pattern) :: _)(config)
  }

  final case class Exclude(pattern: String) extends ConfigOption {
    override def update(config: Configuration): Configuration =
      filters.modify(domain.Filter.exclude(pattern) :: _)(config)
  }

  final case class Debug() extends ConfigOption {
    override def update(config: Configuration): Configuration =
      debug.set(true)(config)
  }

  case object Version extends ConfigOption {
    override def update(config: Configuration): Configuration = config
  }

  case object BatchMode extends ConfigOption {
    override def update(config: Configuration): Configuration =
      batchMode.set(true)(config)
  }

  case object IgnoreUserOptions extends ConfigOption {
    override def update(config: Configuration): Configuration = config
  }

  case object IgnoreGlobalOptions extends ConfigOption {
    override def update(config: Configuration): Configuration = config
  }

  case class Parallel(factor: Int) extends ConfigOption {
    override def update(config: Configuration): Configuration =
      parallel.set(factor)(config)
  }

}
