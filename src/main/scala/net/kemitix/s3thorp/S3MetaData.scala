package net.kemitix.s3thorp

// For the LocalFile, the set of matching S3 objects with the same MD5Hash, and any S3 object with the same remote key
final case class S3MetaData(
  localFile: LocalFile,
  matchByHash: Set[RemoteMetaData],
  matchByKey: Option[RemoteMetaData])
