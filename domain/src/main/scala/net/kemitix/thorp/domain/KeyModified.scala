package net.kemitix.thorp.domain

final case class KeyModified(
    key: RemoteKey,
    modified: LastModified
)
