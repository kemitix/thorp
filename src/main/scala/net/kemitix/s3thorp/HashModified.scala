package net.kemitix.s3thorp

import net.kemitix.s3thorp.domain.MD5Hash

final case class HashModified(hash: MD5Hash,
                              modified: LastModified) {

}
