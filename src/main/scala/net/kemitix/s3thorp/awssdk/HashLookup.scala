package net.kemitix.s3thorp.awssdk

import net.kemitix.s3thorp.{LastModified, MD5Hash, RemoteKey}

/**
  * A list of objects and their MD5 hash values.
  */
case class HashLookup(byHash: Map[MD5Hash, (RemoteKey, LastModified)],
                      byKey: Map[RemoteKey, (MD5Hash, LastModified)]) {

}
