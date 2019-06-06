package net.kemitix.s3thorp.domain

final case class KeyModified(key: RemoteKey,
                             modified: LastModified)
