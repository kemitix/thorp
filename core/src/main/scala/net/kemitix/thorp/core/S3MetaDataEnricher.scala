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
      matchByKey = keyMatches.map { hash =>
        RemoteMetaData(localFile.remoteKey, hash)
      },
      matchByHash = hashMatches.map {
        case (key, hash) => RemoteMetaData(key, hash)
      }
    )
  }

  def getS3Status(
      localFile: LocalFile,
      remoteObjects: RemoteObjects
  ): (Option[MD5Hash], Set[(RemoteKey, MD5Hash)]) = {
    val matchingByKey = remoteObjects.byKey.get(localFile.remoteKey)
    val matchingByHash = localFile.hashes
      .map {
        case (_, md5Hash) =>
          remoteObjects.byHash
            .getOrElse(md5Hash, Set())
            .map(key => (key, md5Hash))
      }
      .flatten
      .toSet
    (matchingByKey, matchingByHash)
  }

}
