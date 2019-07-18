package net.kemitix.thorp.domain

import monocle.macros.Lenses

@Lenses
case class SyncTotals(
    count: Long = 0L,
    totalSizeBytes: Long = 0L,
    sizeUploadedBytes: Long = 0L
)
