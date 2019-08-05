package net.kemitix.thorp.domain

final case class SyncTotals private (
    count: Long,
    totalSizeBytes: Long,
    sizeUploadedBytes: Long
)

object SyncTotals {
  val empty: SyncTotals = SyncTotals(0L, 0L, 0L)
  def create(count: Long,
             totalSizeBytes: Long,
             sizeUploadedBytes: Long): SyncTotals =
    SyncTotals(count, totalSizeBytes, sizeUploadedBytes)
}
