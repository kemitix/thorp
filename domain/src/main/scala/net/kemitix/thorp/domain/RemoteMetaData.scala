package net.kemitix.thorp.domain

final case class RemoteMetaData(
    remoteKey: RemoteKey,
    hash: MD5Hash
)
