package net.kemitix.thorp.core

import monocle.macros.Lenses
import net.kemitix.thorp.domain.LocalFile

@Lenses
case class LocalFiles(
    localFiles: Stream[LocalFile] = Stream(),
    count: Long = 0,
    totalSizeBytes: Long = 0
) {
  def ++(append: LocalFiles): LocalFiles =
    copy(
      localFiles = localFiles ++ append.localFiles,
      count = count + append.count,
      totalSizeBytes = totalSizeBytes + append.totalSizeBytes
    )

}
