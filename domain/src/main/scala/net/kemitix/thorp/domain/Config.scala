package net.kemitix.thorp.domain

import monocle.Lens
import monocle.macros.GenLens

final case class Config(
    bucket: Bucket = Bucket(""),
    prefix: RemoteKey = RemoteKey(""),
    filters: List[Filter] = List(),
    debug: Boolean = false,
    batchMode: Boolean = false,
    sources: Sources = Sources(List())
)

object Config {
  val sources: Lens[Config, Sources]      = GenLens[Config](_.sources)
  val bucket: Lens[Config, Bucket]        = GenLens[Config](_.bucket)
  val prefix: Lens[Config, RemoteKey]     = GenLens[Config](_.prefix)
  val filters: Lens[Config, List[Filter]] = GenLens[Config](_.filters)
  val debug: Lens[Config, Boolean]        = GenLens[Config](_.debug)
  val batchMode: Lens[Config, Boolean]    = GenLens[Config](_.batchMode)
}
