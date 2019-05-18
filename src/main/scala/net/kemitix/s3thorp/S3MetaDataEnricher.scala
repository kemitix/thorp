package net.kemitix.s3thorp

import java.io.File

import net.kemitix.s3thorp.awssdk.{HashLookup, S3Client}

trait S3MetaDataEnricher
  extends S3Client
    with KeyGenerator
    with Logging {

  def enrichWithS3MetaData(file: File)(implicit c: Config, hashLookup: HashLookup): S3MetaData = {
    val key = generateKey(file)(c)
    objectHead(key)
      .map(whenFound(file, key))
      .getOrElse(S3MetaData(localFile = file, remote = None))
  }

  private def whenFound(file: File, remoteKey: RemoteKey): HashModified => S3MetaData = {
    case HashModified(hash, modified) =>
      S3MetaData(file, Some((remoteKey, removeQuotes(hash), modified)))
  }
  private def removeQuotes(in: MD5Hash) = MD5Hash(in.hash.filter({ c=>c!='"'}))
}
