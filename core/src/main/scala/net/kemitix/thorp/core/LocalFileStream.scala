package net.kemitix.thorp.core

import java.io.File
import java.nio.file.Path

import cats.effect.IO
import net.kemitix.thorp.core.KeyGenerator.generateKey
import net.kemitix.thorp.domain
import net.kemitix.thorp.domain._

object LocalFileStream {

  def findFiles(file: File,
                md5HashGenerator: File => IO[MD5Hash])
               (implicit c: Config,
                logger: Logger): IO[Stream[LocalFile]] = {

    val filters: Path => Boolean = Filter.isIncluded(c.filters)

    def loop(file: File): IO[Stream[LocalFile]] = {

      def dirPaths(file: File): IO[Stream[File]] =
        IO(listFiles(file))
          .map(fs =>
            Stream(fs: _*)
              .filter(f => filters(f.toPath)))

      def recurseIntoSubDirectories(file: File)(implicit c: Config): IO[Stream[LocalFile]] =
        file match {
          case f if f.isDirectory => loop(file)
          case _ => for(hash <- md5HashGenerator(file))
            yield Stream(domain.LocalFile(file, c.source, hash, generateKey(c.source, c.prefix)))
        }

      def recurse(fs: Stream[File]): IO[Stream[LocalFile]] =
        fs.foldLeft(IO.pure(Stream.empty[LocalFile]))((acc, f) =>
          recurseIntoSubDirectories(f)
            .flatMap(lfs => acc.map(s => s ++ lfs)))

      for {
        _ <- logger.debug(s"- Entering: $file")
        fs <- dirPaths(file)
        lfs <- recurse(fs)
        _ <- logger.debug(s"- Leaving : $file")
      } yield lfs
    }

    loop(file)
  }

  //TODO: Change this to return an Either[IllegalArgumentException, Array[File]]
  private def listFiles(file: File) = {
    Option(file.listFiles)
      .getOrElse(throw new IllegalArgumentException(s"Directory not found $file"))
  }
}
