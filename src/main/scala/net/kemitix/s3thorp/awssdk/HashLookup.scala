package net.kemitix.s3thorp.awssdk

import net.kemitix.s3thorp.RemoteKey
import net.kemitix.s3thorp.Sync.{LastModified, MD5Hash}

/**
  * A list of objects and their MD5 hash values.
  */
case class HashLookup(byHash: Map[MD5Hash, (RemoteKey, LastModified)],
                      byKey: Map[RemoteKey, (MD5Hash, LastModified)]) {

}
