package net.kemitix.s3thorp.awssdk

import net.kemitix.s3thorp.{HashModified, KeyModified, MD5Hash, RemoteKey}

/**
  * A list of objects and their MD5 hash values.
  */
final case class S3ObjectsData(
  byHash: Map[MD5Hash, Set[KeyModified]],
  byKey: Map[RemoteKey, HashModified]) {

}
