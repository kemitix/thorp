package net.kemitix.s3thorp

import java.io.File

import net.kemitix.s3thorp.awssdk.{HashLookup, S3Client}

trait S3MetaDataEnricher
  extends S3Client
    with KeyGenerator
    with Logging {

  def enrichWithS3MetaData(c: Config)(implicit hashLookup: HashLookup): File => S3MetaData = {
    val remoteKey = generateKey(c)_
    file => {
      val key = remoteKey(file)
      objectHead(key)
        .map(whenFound(file, key))
        .getOrElse(S3MetaData(localFile = file, remote = None))
    }
  }

  private def whenFound(file: File, remoteKey: RemoteKey): HashModified => S3MetaData = {
    case HashModified(hash, modified) =>
      S3MetaData(file, Some((remoteKey, removeQuotes(hash), modified)))
  }
  private def removeQuotes(in: MD5Hash) = MD5Hash(in.hash.filter({ c=>c!='"'}))
}
