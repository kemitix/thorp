package net.kemitix.thorp.domain

/**
  * A list of objects and their MD5 hash values.
  */
final case class RemoteObjects private (
    byHash: Map[MD5Hash, Set[KeyModified]],
    byKey: Map[RemoteKey, HashModified]
)

object RemoteObjects {
  val empty: RemoteObjects = RemoteObjects(Map.empty, Map.empty)
  def create(byHash: Map[MD5Hash, Set[KeyModified]],
             byKey: Map[RemoteKey, HashModified]): RemoteObjects =
    RemoteObjects(byHash, byKey)
}
