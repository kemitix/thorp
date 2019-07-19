package net.kemitix.thorp.domain

/**
  * A list of objects and their MD5 hash values.
  */
final case class S3ObjectsData(
    byHash: Map[MD5Hash, Set[KeyModified]] = Map.empty,
    byKey: Map[RemoteKey, HashModified] = Map.empty
)
