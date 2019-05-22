package net.kemitix.s3thorp

final case class RemoteMetaData(remoteKey: RemoteKey,
                                hash: MD5Hash,
                                lastModified: LastModified) {

}
