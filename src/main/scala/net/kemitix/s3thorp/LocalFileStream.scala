package net.kemitix.s3thorp

import java.io.File

trait LocalFileStream extends Logging {

  def findFiles(file: File)
               (implicit c: Config): Stream[File] = {
    log3(s"- Entering: $file")
    val files = dirPaths(file)
      .map { f => {
        if (f.isFile) log4(s"- Consider: ${c.relativePath(f)}")(c)
        f
      }}
      .flatMap(f => recurseIntoSubDirectories(f))
    log3(s"-  Leaving: $file")
    files
  }

  private def dirPaths(file: File): Stream[File] = {
    Option(file.listFiles)
      .getOrElse(throw new IllegalArgumentException(s"Directory not found $file")).toStream
  }

  private def recurseIntoSubDirectories(file: File)(implicit c: Config): Stream[File] =
    if (file.isDirectory) findFiles(file)(c)
    else Stream(file)

}
