package net.kemitix.s3thorp

import java.io.File

trait LocalFileStream extends Logging {

  def streamDirectoryPaths(c: Config)(file: File): Stream[File] =
    dirPaths(file)
      .map { f => {
        log4(s"- Consider: ${c.relativePath(f)}")(c)
        f
      }}
      .flatMap(f => recurseIntoSubDirectories(c)(f))

  private def dirPaths(file: File): Stream[File] = Option(file.listFiles)
    .getOrElse(throw new IllegalArgumentException(s"Directory not found $file")).toStream

  private def recurseIntoSubDirectories(c: Config): File => Stream[File] =
    file =>
      if (file.isDirectory) streamDirectoryPaths(c)(file)
      else Stream(file)

}
