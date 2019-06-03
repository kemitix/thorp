package net.kemitix.s3thorp

import net.kemitix.s3thorp.domain.{LastModified, MD5Hash, RemoteKey}

final case class RemoteMetaData(remoteKey: RemoteKey,
                                hash: MD5Hash,
                                lastModified: LastModified) {

}
