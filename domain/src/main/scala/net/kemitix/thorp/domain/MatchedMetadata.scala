package net.kemitix.thorp.domain

// For the LocalFile, the set of matching S3 objects with the same MD5Hash, and any S3 object with the same remote key
final case class MatchedMetadata(
    localFile: LocalFile,
    matchByHash: Set[RemoteMetaData], //TODO Can this be an Option?
    matchByKey: Option[RemoteMetaData]
)
