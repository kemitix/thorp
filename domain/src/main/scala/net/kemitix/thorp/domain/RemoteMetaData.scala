package net.kemitix.thorp.domain

import monocle.macros.Lenses

@Lenses
final case class RemoteMetaData(
    remoteKey: RemoteKey,
    hash: MD5Hash,
    lastModified: LastModified
)
