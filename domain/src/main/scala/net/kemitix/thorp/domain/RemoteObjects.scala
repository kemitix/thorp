package net.kemitix.thorp.domain

/**
  * A list of objects and their MD5 hash values.
  */
final case class RemoteObjects private (
    byHash: Map[MD5Hash, Set[RemoteKey]],
    byKey: Map[RemoteKey, MD5Hash]
)

object RemoteObjects {
  val empty: RemoteObjects = RemoteObjects(Map.empty, Map.empty)
  def create(byHash: Map[MD5Hash, Set[RemoteKey]],
             byKey: Map[RemoteKey, MD5Hash]): RemoteObjects =
    RemoteObjects(byHash, byKey)
}
