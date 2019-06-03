package net.kemitix.s3thorp

import net.kemitix.s3thorp.awssdk.{S3Client, S3ObjectsData}
import net.kemitix.s3thorp.domain.{LocalFile, RemoteMetaData, S3MetaData}

trait S3MetaDataEnricher
  extends S3Client
    with Logging {

  def getMetadata(localFile: LocalFile)
                 (implicit c: Config,
                  s3ObjectsData: S3ObjectsData): Stream[S3MetaData] = {
    val (keyMatches, hashMatches) = getS3Status(localFile)
    Stream(
      S3MetaData(localFile,
        matchByKey = keyMatches map { hm => RemoteMetaData(localFile.remoteKey, hm.hash, hm.modified) },
        matchByHash = hashMatches map { km => RemoteMetaData(km.key, localFile.hash, km.modified) }))
  }

}
