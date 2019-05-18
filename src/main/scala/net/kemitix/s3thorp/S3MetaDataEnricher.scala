package net.kemitix.s3thorp

import net.kemitix.s3thorp.awssdk.{S3ObjectsData, S3Client}

trait S3MetaDataEnricher
  extends S3Client
    with Logging {

  def getMetadata(localFile: LocalFile)
                 (implicit c: Config,
                  s3ObjectsData: S3ObjectsData): S3MetaData = {
    val (keyMatches: Option[HashModified], hashMatches: Set[KeyModified]) = getS3Status(localFile)

    S3MetaData(localFile,
      matchByKey = keyMatches.map{kmAsRemoteMetaData(localFile.remoteKey)},
      matchByHash = hashMatches.map(km => RemoteMetaData(km.key, localFile.hash, km.modified)))
  }

  private def kmAsRemoteMetaData(key: RemoteKey): HashModified => RemoteMetaData = hm => RemoteMetaData(key, removeQuotes(hm.hash), hm.modified)

  private def removeQuotes(in: MD5Hash) = MD5Hash(in.hash filter { c => c != '"' })
}
