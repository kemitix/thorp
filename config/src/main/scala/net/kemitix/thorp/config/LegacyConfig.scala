package net.kemitix.thorp.config

import net.kemitix.thorp.domain.{Bucket, Filter, RemoteKey, SimpleLens, Sources}

final case class LegacyConfig(
    bucket: Bucket = Bucket(""),
    prefix: RemoteKey = RemoteKey(""),
    filters: List[Filter] = List(),
    debug: Boolean = false,
    batchMode: Boolean = false,
    sources: Sources = Sources(List())
)

object LegacyConfig {
  val sources: SimpleLens[LegacyConfig, Sources] =
    SimpleLens[LegacyConfig, Sources](_.sources, b => a => b.copy(sources = a))
  val bucket: SimpleLens[LegacyConfig, Bucket] =
    SimpleLens[LegacyConfig, Bucket](_.bucket, b => a => b.copy(bucket = a))
  val prefix: SimpleLens[LegacyConfig, RemoteKey] =
    SimpleLens[LegacyConfig, RemoteKey](_.prefix, b => a => b.copy(prefix = a))
  val filters: SimpleLens[LegacyConfig, List[Filter]] =
    SimpleLens[LegacyConfig, List[Filter]](_.filters,
                                           b => a => b.copy(filters = a))
  val debug: SimpleLens[LegacyConfig, Boolean] =
    SimpleLens[LegacyConfig, Boolean](_.debug, b => a => b.copy(debug = a))
  val batchMode: SimpleLens[LegacyConfig, Boolean] =
    SimpleLens[LegacyConfig, Boolean](_.batchMode,
                                      b => a => b.copy(batchMode = a))
}
