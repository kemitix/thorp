package net.kemitix.thorp.domain

import monocle.macros.Lenses

@Lenses
final case class Bucket(
    name: String
)
