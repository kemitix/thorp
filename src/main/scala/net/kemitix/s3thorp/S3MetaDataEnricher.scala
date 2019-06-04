package net.kemitix.s3thorp

import net.kemitix.s3thorp.domain._

object S3MetaDataEnricher {

  def getMetadata(localFile: LocalFile)
                 (implicit c: Config,
                  s3ObjectsData: S3ObjectsData): Stream[S3MetaData] = {
    val (keyMatches, hashMatches) = getS3Status(localFile)
    Stream(
      S3MetaData(localFile,
        matchByKey = keyMatches map { hm => RemoteMetaData(localFile.remoteKey, hm.hash, hm.modified) },
        matchByHash = hashMatches map { km => RemoteMetaData(km.key, localFile.hash, km.modified) }))
  }

  def getS3Status(localFile: LocalFile)
                         (implicit s3ObjectsData: S3ObjectsData): (Option[HashModified], Set[KeyModified]) = {
    val matchingByKey = s3ObjectsData.byKey.get(localFile.remoteKey)
    val matchingByHash = s3ObjectsData.byHash.getOrElse(localFile.hash, Set())
    (matchingByKey, matchingByHash)
  }

}
