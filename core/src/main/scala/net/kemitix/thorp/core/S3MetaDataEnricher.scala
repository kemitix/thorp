package net.kemitix.thorp.core

import net.kemitix.thorp.domain._

object S3MetaDataEnricher {

  def getMetadata(
      localFile: LocalFile,
      s3ObjectsData: S3ObjectsData
  )(implicit c: Config): S3MetaData = {
    val (keyMatches, hashMatches) = getS3Status(localFile, s3ObjectsData)
    S3MetaData(
      localFile,
      matchByKey = keyMatches.map { hm =>
        RemoteMetaData(localFile.remoteKey, hm.hash, hm.modified)
      },
      matchByHash = hashMatches.map {
        case (hash, km) => RemoteMetaData(km.key, hash, km.modified)
      }
    )
  }

  def getS3Status(
      localFile: LocalFile,
      s3ObjectsData: S3ObjectsData
  ): (Option[HashModified], Set[(MD5Hash, KeyModified)]) = {
    val matchingByKey = s3ObjectsData.byKey.get(localFile.remoteKey)
    val matchingByHash = localFile.hashes
      .map {
        case (_, md5Hash) =>
          s3ObjectsData.byHash
            .getOrElse(md5Hash, Set())
            .map(km => (md5Hash, km))
      }
      .flatten
      .toSet
    (matchingByKey, matchingByHash)
  }

}
