package net.kemitix.thorp.lib

import net.kemitix.thorp.domain._

object S3MetaDataEnricher {

  def getMetadata(
      localFile: LocalFile,
      remoteObjects: RemoteObjects
  ): MatchedMetadata = {
    val (keyMatches, hashMatches) = getS3Status(localFile, remoteObjects)

    val maybeByKey: Option[RemoteMetaData] = keyMatches.map { hash =>
      RemoteMetaData(localFile.remoteKey, hash)
    }

    val maybeByHash: Option[RemoteMetaData] = hashMatches.map {
      case (md5Hash, remoteKey) =>
        RemoteMetaData(remoteKey, md5Hash)
    }.headOption

    MatchedMetadata(
      localFile,
      matchByKey = maybeByKey,
      matchByHash = maybeByHash
    )
  }

  def getS3Status(
      localFile: LocalFile,
      remoteObjects: RemoteObjects
  ): (Option[MD5Hash], Map[MD5Hash, RemoteKey]) = {
    val byKey: Option[MD5Hash] =
      remoteObjects.byKey.get(localFile.remoteKey)
    val hashes: Map[HashType, MD5Hash] = localFile.hashes
    val byHash: Map[MD5Hash, RemoteKey] =
      hashes
        .map {
          case (hashType, md5Hash) =>
            (md5Hash, remoteObjects.byHash.get(md5Hash))
        }
        .flatMap {
          case (md5Hash, Some(maybeyKey)) => Some((md5Hash, maybeyKey))
          case (_, None)                  => None
        }

    (byKey, byHash)
  }

}
