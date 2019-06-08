package net.kemitix.s3thorp.core

import net.kemitix.s3thorp.domain._

object S3MetaDataEnricher {

  def getMetadata(localFile: LocalFile,
                  s3ObjectsData: S3ObjectsData)
                 (implicit c: Config): S3MetaData = {
    val (keyMatches, hashMatches) = getS3Status(localFile, s3ObjectsData)
    S3MetaData(localFile,
      matchByKey = keyMatches map { hm => RemoteMetaData(localFile.remoteKey, hm.hash, hm.modified) },
      matchByHash = hashMatches map { km => RemoteMetaData(km.key, localFile.hash, km.modified) })
  }

  def getS3Status(localFile: LocalFile,
                  s3ObjectsData: S3ObjectsData): (Option[HashModified], Set[KeyModified]) = {
    val matchingByKey = s3ObjectsData.byKey.get(localFile.remoteKey)
    val matchingByHash = s3ObjectsData.byHash.getOrElse(localFile.hash, Set())
    (matchingByKey, matchingByHash)
  }

}
