package net.kemitix.thorp.domain

case class SyncTotals private (
    count: Long,
    totalSizeBytes: Long,
    sizeUploadedBytes: Long
)

object SyncTotals {
  def empty: SyncTotals = SyncTotals(0L, 0L, 0L)
  def create(count: Long, totalSizeBytes: Long, sizeUploadedBytes: Long) =
    SyncTotals(count, totalSizeBytes, sizeUploadedBytes)
}
