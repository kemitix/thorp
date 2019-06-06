package net.kemitix.s3thorp.domain

/**
  * A list of objects and their MD5 hash values.
  */
final case class S3ObjectsData(byHash: Map[MD5Hash, Set[KeyModified]],
                               byKey: Map[RemoteKey, HashModified])
