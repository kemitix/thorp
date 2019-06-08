package net.kemitix.s3thorp.core

import java.io.File

import cats.effect.IO
import net.kemitix.s3thorp.core.KeyGenerator.generateKey
import net.kemitix.s3thorp.domain.{Config, LocalFile, MD5Hash}

object LocalFileStream {

  def findFiles(file: File,
                md5HashGenerator: File => IO[MD5Hash],
                info: Int => String => Unit)
               (implicit c: Config): IO[Stream[LocalFile]] = {

    def loop(file: File): IO[Stream[LocalFile]] = {

      def dirPaths(file: File): IO[Stream[File]] =
        IO {
          Option(file.listFiles)
            .getOrElse(throw new IllegalArgumentException(s"Directory not found $file"))
        }
          .map(fs =>
            Stream(fs: _*)
              .filter(isIncluded))

      def recurseIntoSubDirectories(file: File)(implicit c: Config): IO[Stream[LocalFile]] =
        file match {
          case f if f.isDirectory => loop(file)
          case _ => for(hash <- md5HashGenerator(file))
            yield Stream(LocalFile(file, c.source, hash, generateKey(c.source, c.prefix)))
        }

      def filterIsIncluded(f: File): Boolean =
        f.isDirectory || c.includes.forall(_.isIncluded(f.toPath))

      def excludeIsIncluded(f: File): Boolean =
        c.excludes.forall(_.isIncluded(f.toPath))

      def isIncluded(f: File): Boolean =
        filterIsIncluded(f) && excludeIsIncluded(f)

      def recurse(fs: Stream[File]): IO[Stream[LocalFile]] =
        fs.foldLeft(IO.pure(Stream.empty[LocalFile]))((acc, f) =>
          recurseIntoSubDirectories(f)
            .flatMap(lfs => acc.map(s => s ++ lfs)))

      for {
        _ <- IO(info(2)(s"- Entering: $file"))
        fs <- dirPaths(file)
        lfs <- recurse(fs)
        _ <- IO(info(5)(s"- Leaving : $file"))
      } yield lfs
    }

    loop(file)
  }
}
