package net.kemitix.s3thorp.domain

final case class RemoteMetaData(remoteKey: RemoteKey,
                                hash: MD5Hash,
                                lastModified: LastModified) {

}
