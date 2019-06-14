package net.kemitix.s3thorp.core

import java.io.File
import java.nio.file.Path

import cats.Monad
import cats.implicits._
import net.kemitix.s3thorp.core.KeyGenerator.generateKey
import net.kemitix.s3thorp.domain.{Config, Filter, LocalFile, MD5Hash}

object LocalFileStream {

  def findFiles[M[_]: Monad](file: File,
                md5HashGenerator: File => M[MD5Hash],
                info: Int => String => M[Unit])
               (implicit c: Config): M[Stream[LocalFile]] = {

    val filters: Path => Boolean = Filter.isIncluded(c.filters)

    def loop(file: File): M[Stream[LocalFile]] = {

      def dirPaths(file: File): M[Stream[File]] =
        Monad[M].pure {
          Option(file.listFiles)
            .getOrElse(throw new IllegalArgumentException(s"Directory not found $file"))
        }
          .map(fs =>
            Stream(fs: _*)
              .filter(f => filters(f.toPath)))

      def recurseIntoSubDirectories(file: File)(implicit c: Config): M[Stream[LocalFile]] =
        file match {
          case f if f.isDirectory => loop(file)
          case _ => for(hash <- md5HashGenerator(file))
            yield Stream(LocalFile(file, c.source, hash, generateKey(c.source, c.prefix)))
        }

      def recurse(fs: Stream[File]): M[Stream[LocalFile]] =
        fs.foldLeft(Monad[M].pure(Stream.empty[LocalFile]))((acc, f) =>
          recurseIntoSubDirectories(f)
            .flatMap(lfs => acc.map(s => s ++ lfs)))

      for {
        _ <- info(2)(s"- Entering: $file")
        fs <- dirPaths(file)
        lfs <- recurse(fs)
        _ <- info(5)(s"- Leaving : $file")
      } yield lfs
    }

    loop(file)
  }
}
