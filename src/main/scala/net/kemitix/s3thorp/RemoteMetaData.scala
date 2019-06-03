package net.kemitix.s3thorp

import net.kemitix.s3thorp.domain.RemoteKey

final case class RemoteMetaData(remoteKey: RemoteKey,
                                hash: MD5Hash,
                                lastModified: LastModified) {

}
