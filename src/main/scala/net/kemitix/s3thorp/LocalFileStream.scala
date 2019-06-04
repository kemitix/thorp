package net.kemitix.s3thorp

import java.io.File

import net.kemitix.s3thorp.domain.{Config, LocalFile, MD5Hash}

class LocalFileStream(md5HashGenerator: File => MD5Hash,
                      info: Int => String => Unit)
  extends KeyGenerator {

  def findFiles(file: File)
               (implicit c: Config): Stream[LocalFile] = {
    info(2)(s"- Entering: $file")
    val files = for {
      f <- dirPaths(file)
        .filter { f => f.isDirectory || c.filters.forall { filter => filter isIncluded f.toPath } }
        .filter { f => c.excludes.forall { exclude => exclude isIncluded f.toPath } }
      fs <- recurseIntoSubDirectories(f)
      } yield fs
    info(5)(s"-  Leaving: $file")
    files
  }

  private def dirPaths(file: File): Stream[File] = {
    Option(file.listFiles)
      .getOrElse(throw new IllegalArgumentException(s"Directory not found $file")).toStream
  }

  private def recurseIntoSubDirectories(file: File)(implicit c: Config): Stream[LocalFile] =
    if (file.isDirectory) findFiles(file)(c)
    else Stream(LocalFile(file, c.source, generateKey(c.source, c.prefix), md5HashGenerator))

}
