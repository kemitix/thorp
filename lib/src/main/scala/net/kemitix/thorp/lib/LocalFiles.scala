package net.kemitix.thorp.lib

import net.kemitix.thorp.domain.LocalFile

final case class LocalFiles(
    localFiles: LazyList[LocalFile],
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
  val empty: LocalFiles = LocalFiles(LazyList.empty, 0L, 0L)
  def reduce: LazyList[LocalFiles] => LocalFiles =
    list => list.foldLeft(LocalFiles.empty)((acc, lf) => acc ++ lf)
  def one(localFile: LocalFile): LocalFiles =
    LocalFiles(LazyList(localFile), 1, localFile.file.length)
}
