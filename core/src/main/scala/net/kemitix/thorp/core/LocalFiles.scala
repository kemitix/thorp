package net.kemitix.thorp.core

import net.kemitix.thorp.domain.LocalFile

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

object LocalFiles {
  def reduce: Stream[LocalFiles] => LocalFiles =
    list => list.foldLeft(LocalFiles())((acc, lf) => acc ++ lf)
  def one(localFile: LocalFile): LocalFiles =
    LocalFiles(Stream(localFile), 1, localFile.file.length)
}
