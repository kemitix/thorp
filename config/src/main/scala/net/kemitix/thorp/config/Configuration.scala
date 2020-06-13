package net.kemitix.thorp.config

import net.kemitix.thorp.domain.{Bucket, Filter, RemoteKey, Sources}

private[config] final case class Configuration(
    bucket: Bucket,
    prefix: RemoteKey,
    filters: List[Filter],
    debug: Boolean,
    batchMode: Boolean,
    parallel: Int,
    sources: Sources
)

private[config] object Configuration {
  val empty: Configuration = Configuration(
    bucket = Bucket.named(""),
    prefix = RemoteKey.create(""),
    filters = List.empty,
    debug = false,
    batchMode = false,
    parallel = 1,
    sources = Sources.emptySources
  )
  val sources: SimpleLens[Configuration, Sources] =
    SimpleLens[Configuration, Sources](_.sources, b => a => b.copy(sources = a))
  val bucket: SimpleLens[Configuration, Bucket] =
    SimpleLens[Configuration, Bucket](_.bucket, b => a => b.copy(bucket = a))
  val prefix: SimpleLens[Configuration, RemoteKey] =
    SimpleLens[Configuration, RemoteKey](_.prefix, b => a => b.copy(prefix = a))
  val filters: SimpleLens[Configuration, List[Filter]] =
    SimpleLens[Configuration, List[Filter]](_.filters,
                                            b => a => b.copy(filters = a))
  val debug: SimpleLens[Configuration, Boolean] =
    SimpleLens[Configuration, Boolean](_.debug, b => a => b.copy(debug = a))
  val batchMode: SimpleLens[Configuration, Boolean] =
    SimpleLens[Configuration, Boolean](_.batchMode,
                                       b => a => b.copy(batchMode = a))
  val parallel: SimpleLens[Configuration, Int] =
    SimpleLens[Configuration, Int](_.parallel, b => a => b.copy(parallel = a))
}
