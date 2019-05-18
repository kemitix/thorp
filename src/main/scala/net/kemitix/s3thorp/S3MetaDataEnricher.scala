package net.kemitix.s3thorp

import net.kemitix.s3thorp.awssdk.{HashLookup, S3Client}

trait S3MetaDataEnricher
  extends S3Client
    with Logging {

  def getMetadata(localFile: LocalFile)
                 (implicit c: Config, hashLookup: HashLookup): S3MetaData = {
    objectHead(localFile.remoteKey)
      .map(asS3MetaData(localFile))
      .getOrElse(S3MetaData(localFile = localFile, remote = None))
  }

  private def asS3MetaData(localFile: LocalFile)
                       (implicit c: Config): HashModified => S3MetaData = {
    case HashModified(hash, modified) =>
      S3MetaData(localFile, Some(RemoteMetaData(localFile.remoteKey, removeQuotes(hash), modified)))
  }
  private def removeQuotes(in: MD5Hash) = MD5Hash(in.hash filter { c => c != '"' })
}
