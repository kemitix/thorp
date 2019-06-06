package net.kemitix.s3thorp.domain

final case class HashModified(hash: MD5Hash,
                              modified: LastModified)
