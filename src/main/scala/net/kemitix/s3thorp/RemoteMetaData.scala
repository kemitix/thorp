package net.kemitix.s3thorp

import net.kemitix.s3thorp.domain.{MD5Hash, RemoteKey}

final case class RemoteMetaData(remoteKey: RemoteKey,
                                hash: MD5Hash,
                                lastModified: LastModified) {

}
