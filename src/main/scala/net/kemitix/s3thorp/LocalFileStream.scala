package net.kemitix.s3thorp

import java.io.File

import net.kemitix.s3thorp.KeyGenerator.generateKey
import net.kemitix.s3thorp.domain.{Config, LocalFile, MD5Hash}

object LocalFileStream {

  def findFiles(file: File,
                md5HashGenerator: File => MD5Hash,
                info: Int => String => Unit)
               (implicit c: Config): Stream[LocalFile] = {
    def loop(file: File): Stream[LocalFile] = {
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

    def dirPaths(file: File): Stream[File] =
      Option(file.listFiles)
        .getOrElse(throw new IllegalArgumentException(s"Directory not found $file")).toStream

    def recurseIntoSubDirectories(file: File)(implicit c: Config): Stream[LocalFile] =
      if (file.isDirectory) loop(file)
      else Stream(LocalFile(file, c.source, generateKey(c.source, c.prefix), md5HashGenerator))

    loop(file)
  }
}
