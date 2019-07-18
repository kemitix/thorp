package net.kemitix.thorp.domain

import monocle.macros.Lenses

/**
  * A list of objects and their MD5 hash values.
  */

@Lenses
final case class S3ObjectsData(
    byHash: Map[MD5Hash, Set[KeyModified]] = Map.empty,
    byKey: Map[RemoteKey, HashModified] = Map.empty
)
