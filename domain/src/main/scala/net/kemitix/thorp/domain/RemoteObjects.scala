package net.kemitix.thorp.domain

import zio.UIO

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

  def remoteKeyExists(
      remoteObjects: RemoteObjects,
      remoteKey: RemoteKey
  ): UIO[Boolean] = UIO(remoteObjects.byKey.contains(remoteKey))

  def remoteMatchesLocalFile(
      remoteObjects: RemoteObjects,
      localFile: LocalFile
  ): UIO[Boolean] =
    UIO(
      remoteObjects.byKey
        .get(localFile.remoteKey)
        .exists(LocalFile.matchesHash(localFile)))

  def remoteHasHash(
      remoteObjects: RemoteObjects,
      hashes: Hashes
  ): UIO[Option[(RemoteKey, MD5Hash)]] =
    UIO(remoteObjects.byHash.collectFirst {
      case (hash, key) if (hashes.values.exists(h => h == hash)) => (key, hash)
    })

}
