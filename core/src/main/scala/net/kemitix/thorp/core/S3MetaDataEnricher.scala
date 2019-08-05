package net.kemitix.thorp.core

import net.kemitix.thorp.domain._

object S3MetaDataEnricher {

  def getMetadata(
      localFile: LocalFile,
      remoteObjects: RemoteObjects
  ): MatchedMetadata = {
    val (keyMatches, hashMatches) = getS3Status(localFile, remoteObjects)
    MatchedMetadata(
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
      remoteObjects: RemoteObjects
  ): (Option[HashModified], Set[(MD5Hash, KeyModified)]) = {
    val matchingByKey = remoteObjects.byKey.get(localFile.remoteKey)
    val matchingByHash = localFile.hashes
      .map {
        case (_, md5Hash) =>
          remoteObjects.byHash
            .getOrElse(md5Hash, Set())
            .map(km => (md5Hash, km))
      }
      .flatten
      .toSet
    (matchingByKey, matchingByHash)
  }

}
