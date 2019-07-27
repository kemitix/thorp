package net.kemitix.thorp.core

import java.nio.file.Path

import net.kemitix.thorp.config.LegacyConfig
import net.kemitix.thorp.core.KeyGenerator.generateKey
import net.kemitix.thorp.domain
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.HashService
import zio.Task

object LocalFileStream {

  def findFiles(
      source: Path,
      hashService: HashService
  )(
      implicit c: LegacyConfig
  ): Task[LocalFiles] = {

    val isIncluded: Path => Boolean = Filter.isIncluded(c.filters)

    val pathToLocalFile: Path => Task[LocalFiles] = path =>
      localFile(hashService, c)(path)

    def loop(path: Path): Task[LocalFiles] = {

      def dirPaths(path: Path): Task[Stream[Path]] =
        listFiles(path)
          .map(_.filter(isIncluded))

      def recurseIntoSubDirectories(path: Path): Task[LocalFiles] =
        path.toFile match {
          case f if f.isDirectory => loop(path)
          case _                  => pathToLocalFile(path)
        }

      def recurse(paths: Stream[Path]): Task[LocalFiles] =
        Task.foldLeft(paths)(LocalFiles())((acc, path) => {
          recurseIntoSubDirectories(path).map(localFiles => acc ++ localFiles)
        })

      for {
        paths      <- dirPaths(path)
        localFiles <- recurse(paths)
      } yield localFiles
    }

    loop(source)
  }

  def localFile(
      hashService: HashService,
      c: LegacyConfig
  ): Path => Task[LocalFiles] =
    path => {
      val file   = path.toFile
      val source = c.sources.forPath(path)
      for {
        hash <- hashService.hashLocalObject(path)
      } yield
        LocalFiles(localFiles = Stream(
                     domain.LocalFile(file,
                                      source.toFile,
                                      hash,
                                      generateKey(c.sources, c.prefix)(path))),
                   count = 1,
                   totalSizeBytes = file.length)
    }

  private def listFiles(path: Path): Task[Stream[Path]] =
    for {
      files <- Task(path.toFile.listFiles)
      _ <- Task.when(files == null)(
        Task.fail(new IllegalArgumentException(s"Directory not found $path")))
    } yield Stream(files: _*).map(_.toPath)

}
