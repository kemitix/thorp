package net.kemitix.thorp.core

import java.io.File
import java.nio.file.Path

import cats.effect.IO
import net.kemitix.thorp.core.KeyGenerator.generateKey
import net.kemitix.thorp.domain
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.HashService

object LocalFileStream {

  def findFiles(file: File,
                hashService: HashService)
               (implicit c: Config,
                logger: Logger): IO[LocalFiles] = {

    val filters: Path => Boolean = Filter.isIncluded(c.filters)

    def loop(file: File): IO[LocalFiles] = {

      def dirPaths(file: File): IO[Stream[File]] =
        IO(listFiles(file))
          .map(fs =>
            Stream(fs: _*)
              .filter(f => filters(f.toPath)))

      def recurseIntoSubDirectories(file: File): IO[LocalFiles] =
        file match {
          case f if f.isDirectory => loop(file)
          case _ => localFile(hashService, file)
        }

      def recurse(fs: Stream[File]): IO[LocalFiles] =
        fs.foldLeft(IO.pure(LocalFiles()))((acc, f) =>
          recurseIntoSubDirectories(f)
            .flatMap(localFiles => acc.map(accLocalFiles => accLocalFiles ++ localFiles)))

      for {
        _ <- logger.debug(s"- Entering: $file")
        fs <- dirPaths(file)
        lfs <- recurse(fs)
        _ <- logger.debug(s"- Leaving : $file")
      } yield lfs
    }

    loop(file)
  }

  private def localFile(hashService: HashService, file: File)(implicit l: Logger, c: Config) = {
    for {
      hash <- hashService.hashLocalObject(file)
    } yield LocalFiles(Stream(domain.LocalFile(file, c.source, hash, generateKey(c.source, c.prefix)(file))))
  }

  //TODO: Change this to return an Either[IllegalArgumentException, Array[File]]
  private def listFiles(file: File) = {
    Option(file.listFiles)
      .getOrElse(throw new IllegalArgumentException(s"Directory not found $file"))
  }
}
