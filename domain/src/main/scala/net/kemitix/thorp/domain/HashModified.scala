package net.kemitix.thorp.domain

import monocle.macros.Lenses

@Lenses
final case class HashModified(
    hash: MD5Hash,
    modified: LastModified
)
