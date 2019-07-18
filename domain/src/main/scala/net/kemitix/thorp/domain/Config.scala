package net.kemitix.thorp.domain

import monocle.macros.Lenses

@Lenses
final case class Config(
    bucket: Bucket = Bucket(""),
    prefix: RemoteKey = RemoteKey(""),
    filters: List[Filter] = List(),
    debug: Boolean = false,
    batchMode: Boolean = false,
    sources: Sources = Sources(List())
)
