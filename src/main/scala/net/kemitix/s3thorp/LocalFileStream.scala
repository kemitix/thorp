package net.kemitix.s3thorp

import java.io.File

trait LocalFileStream
  extends KeyGenerator
    with Logging {

  def findFiles(file: File)
               (implicit c: Config): Stream[LocalFile] = {
    log2(s"- Entering: $file")
    val files = for {
      f <- dirPaths(file)
        .filter { f => f.isDirectory || c.filters.forall { filter => filter isIncluded f.toPath } }
        .filter { f => c.excludes.forall { exclude => exclude isIncluded f.toPath } }
      fs <- recurseIntoSubDirectories(f)
      } yield fs
    log5(s"-  Leaving: $file")
    files
  }

  private def dirPaths(file: File): Stream[File] = {
    Option(file.listFiles)
      .getOrElse(throw new IllegalArgumentException(s"Directory not found $file")).toStream
  }

  private def recurseIntoSubDirectories(file: File)(implicit c: Config): Stream[LocalFile] =
    if (file.isDirectory) findFiles(file)(c)
    else Stream(LocalFile(file, c.source, generateKey(c.source, c.prefix)))

}
