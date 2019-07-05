package net.kemitix.thorp.core

import java.io.File
import java.nio.file.Path

import cats.effect.IO
import net.kemitix.thorp.core.KeyGenerator.generateKey
import net.kemitix.thorp.domain
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.HashService

object LocalFileStream {

  def findFiles(source: Path,
                hashService: HashService)
               (implicit c: Config,
                logger: Logger): IO[LocalFiles] = {

    val isIncluded: Path => Boolean = Filter.isIncluded(c.filters)

    def loop(path: Path): IO[LocalFiles] = {

      def dirPaths(path: Path): IO[Stream[Path]] =
        IO(listFiles(path))
          .map(fs =>
            Stream(fs: _*)
              .map(_.toPath)
              .filter(isIncluded))

      def recurseIntoSubDirectories(path: Path): IO[LocalFiles] =
        path.toFile match {
          case f if f.isDirectory => loop(path)
          case _ => localFile(hashService, path)
        }

      def recurse(paths: Stream[Path]): IO[LocalFiles] =
        paths.foldLeft(IO.pure(LocalFiles()))((acc, path) =>
          recurseIntoSubDirectories(path)
            .flatMap(localFiles => acc.map(accLocalFiles => accLocalFiles ++ localFiles)))

      for {
        _ <- logger.debug(s"- Entering: $path")
        fs <- dirPaths(path)
        lfs <- recurse(fs)
        _ <- logger.debug(s"- Leaving : $path")
      } yield lfs
    }

    loop(source)
  }

  private def localFile(hashService: HashService,
                        path: Path)
                       (implicit l: Logger, c: Config) = {
    val file = path.toFile
    for {
      hash <- hashService.hashLocalObject(path)
    } yield
      LocalFiles(
        localFiles = Stream(
          domain.LocalFile(
            file,
            c.source.toFile,
            hash,
            generateKey(c.source, c.prefix)(path))),
        count = 1,
        totalSizeBytes = file.length)
  }

  //TODO: Change this to return an Either[IllegalArgumentException, Array[File]]
  private def listFiles(path: Path): Array[File] = {
    Option(path.toFile.listFiles)
      .getOrElse(throw new IllegalArgumentException(s"Directory not found $path"))
  }
}
