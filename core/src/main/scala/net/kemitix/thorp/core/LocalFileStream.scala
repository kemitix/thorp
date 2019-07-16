package net.kemitix.thorp.core

import java.nio.file.Path

import cats.effect.IO
import net.kemitix.thorp.core.KeyGenerator.generateKey
import net.kemitix.thorp.domain
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.HashService

object LocalFileStream {

  private val emptyIOLocalFiles = IO.pure(LocalFiles())

  def findFiles(
      source: Path,
      hashService: HashService
  )(
      implicit c: Config,
      logger: Logger
  ): IO[LocalFiles] = {

    val isIncluded: Path => Boolean = Filter.isIncluded(c.filters)

    val pathToLocalFile: Path => IO[LocalFiles] = path =>
      localFile(hashService, logger, c)(path)

    def loop(path: Path): IO[LocalFiles] = {

      def dirPaths(path: Path) =
        listFiles(path)
          .map(_.filter(isIncluded))

      def recurseIntoSubDirectories(path: Path) =
        path.toFile match {
          case f if f.isDirectory => loop(path)
          case _                  => pathToLocalFile(path)
        }

      def recurse(paths: Stream[Path]) =
        paths.foldLeft(emptyIOLocalFiles)(
          (acc, path) =>
            recurseIntoSubDirectories(path)
              .flatMap(localFiles =>
                acc.map(accLocalFiles => accLocalFiles ++ localFiles)))

      for {
        _          <- logger.debug(s"- Entering: $path")
        paths      <- dirPaths(path)
        localFiles <- recurse(paths)
        _          <- logger.debug(s"- Leaving : $path")
      } yield localFiles
    }

    loop(source)
  }

  def localFile(
      hashService: HashService,
      l: Logger,
      c: Config
  ): Path => IO[LocalFiles] =
    path => {
      val file   = path.toFile
      val source = c.sources.forPath(path)
      for {
        hash <- hashService.hashLocalObject(path)(l)
      } yield
        LocalFiles(localFiles = Stream(
                     domain.LocalFile(file,
                                      source.toFile,
                                      hash,
                                      generateKey(c.sources, c.prefix)(path))),
                   count = 1,
                   totalSizeBytes = file.length)
    }

  //TODO: Change this to return an Either[IllegalArgumentException, Stream[Path]]
  private def listFiles(path: Path) = {
    IO(
      Option(path.toFile.listFiles)
        .map { fs =>
          Stream(fs: _*)
            .map(_.toPath)
        }
        .getOrElse(
          throw new IllegalArgumentException(s"Directory not found $path")))
  }
}
