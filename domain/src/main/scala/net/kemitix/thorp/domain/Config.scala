package net.kemitix.thorp.domain

final case class Config(
    bucket: Bucket = Bucket(""),
    prefix: RemoteKey = RemoteKey(""),
    filters: List[Filter] = List(),
    debug: Boolean = false,
    batchMode: Boolean = false,
    sources: Sources = Sources(List())
)

object Config {
  val sources: SimpleLens[Config, Sources] =
    SimpleLens[Config, Sources](_.sources, b => a => b.copy(sources = a))
  val bucket: SimpleLens[Config, Bucket] =
    SimpleLens[Config, Bucket](_.bucket, b => a => b.copy(bucket = a))
  val prefix: SimpleLens[Config, RemoteKey] =
    SimpleLens[Config, RemoteKey](_.prefix, b => a => b.copy(prefix = a))
  val filters: SimpleLens[Config, List[Filter]] =
    SimpleLens[Config, List[Filter]](_.filters, b => a => b.copy(filters = a))
  val debug: SimpleLens[Config, Boolean] =
    SimpleLens[Config, Boolean](_.debug, b => a => b.copy(debug = a))
  val batchMode: SimpleLens[Config, Boolean] =
    SimpleLens[Config, Boolean](_.batchMode, b => a => b.copy(batchMode = a))
}
