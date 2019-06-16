package net.kemitix.thorp.domain

final case class HashModified(hash: MD5Hash,
                              modified: LastModified)
