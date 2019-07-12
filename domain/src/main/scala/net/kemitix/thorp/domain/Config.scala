package net.kemitix.thorp.domain

final case class Config(
    bucket: Bucket = Bucket(""),
    prefix: RemoteKey = RemoteKey(""),
    filters: List[Filter] = List(),
    debug: Boolean = false,
    batchMode: Boolean = false,
    sources: Sources = Sources(List())
)
