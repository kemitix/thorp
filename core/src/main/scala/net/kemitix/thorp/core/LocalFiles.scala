package net.kemitix.thorp.core

import net.kemitix.thorp.domain.LocalFile

case class LocalFiles(localFiles: Stream[LocalFile] = Stream()) {
  def ++(append: LocalFiles): LocalFiles =
    copy(localFiles = localFiles ++ append.localFiles)

}

