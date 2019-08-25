package net.kemitix.thorp.domain

import scala.collection.MapView

/**
  * A list of objects and their MD5 hash values.
  */
final case class RemoteObjects private (
    byHash: MapView[MD5Hash, RemoteKey],
    byKey: MapView[RemoteKey, MD5Hash]
)

object RemoteObjects {
  val empty: RemoteObjects = RemoteObjects(MapView.empty, MapView.empty)
  def create(byHash: MapView[MD5Hash, RemoteKey],
             byKey: MapView[RemoteKey, MD5Hash]): RemoteObjects =
    RemoteObjects(byHash, byKey)
}
