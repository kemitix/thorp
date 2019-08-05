package net.kemitix.thorp.domain

/**
  * A list of objects and their MD5 hash values.
  */
final case class RemoteObjects(
    byHash: Map[MD5Hash, Set[KeyModified]] = Map.empty,
    byKey: Map[RemoteKey, HashModified] = Map.empty
)
