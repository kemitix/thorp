package net.kemitix.s3thorp

import java.io.File

trait LocalFileStream {

  def streamDirectoryPaths(file: File): Stream[File] =
    dirPaths(file)
        .flatMap(f => recurseIntoSubDirectories(f))

  private def dirPaths(file: File): Stream[File] = Option(file.listFiles)
    .getOrElse(throw new IllegalArgumentException(s"Directory not found $file")).toStream

  private def recurseIntoSubDirectories: File => Stream[File] =
    file =>
      if (file.isDirectory) streamDirectoryPaths(file)
      else Stream(file)

}
