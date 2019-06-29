package net.kemitix.thorp.core

import net.kemitix.thorp.domain._

object S3MetaDataEnricher {

  def getMetadata(localFile: LocalFile,
                  s3ObjectsData: S3ObjectsData)
                 (implicit c: Config): S3MetaData = {
    val (keyMatches, hashMatches) = getS3Status(localFile, s3ObjectsData)
    S3MetaData(localFile,
      matchByKey = keyMatches map { hm => RemoteMetaData(localFile.remoteKey, hm.hash, hm.modified) },
      matchByHash = hashMatches map { case (hash, km) => RemoteMetaData(km.key, hash, km.modified) })
  }

  def getS3Status(localFile: LocalFile,
                  s3ObjectsData: S3ObjectsData): (Option[HashModified], Set[(MD5Hash, KeyModified)]) = {
    val matchingByKey = s3ObjectsData.byKey.get(localFile.remoteKey)
    val matchingByHash = localFile.hashes.map(hash => {
      s3ObjectsData.byHash.getOrElse(hash._2, Set())
        .map(km => (hash._2, km))
    }).flatten.toSet
    (matchingByKey, matchingByHash)
  }

}
