package net.kemitix.thorp.core

import net.kemitix.thorp.domain.LocalFile

final case class LocalFiles(
    localFiles: Stream[LocalFile],
    count: Long,
    totalSizeBytes: Long
) {
  def ++(append: LocalFiles): LocalFiles =
    copy(
      localFiles = localFiles ++ append.localFiles,
      count = count + append.count,
      totalSizeBytes = totalSizeBytes + append.totalSizeBytes
    )
}

object LocalFiles {
  val empty: LocalFiles = LocalFiles(Stream.empty, 0L, 0L)
  def reduce: Stream[LocalFiles] => LocalFiles =
    list => list.foldLeft(LocalFiles.empty)((acc, lf) => acc ++ lf)
  def one(localFile: LocalFile): LocalFiles =
    LocalFiles(Stream(localFile), 1, localFile.file.length)
}
