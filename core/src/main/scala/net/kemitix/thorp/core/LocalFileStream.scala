package net.kemitix.thorp.core

import java.nio.file.Path

import net.kemitix.thorp.config._
import net.kemitix.thorp.core.KeyGenerator.generateKey
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.HashService
import zio.{Task, TaskR, ZIO}

object LocalFileStream {

  def findFiles(hashService: HashService)(
      source: Path
  ): TaskR[Config, LocalFiles] = {

    val isIncluded: Path => TaskR[Config, Boolean] =
      path =>
        for {
          filters <- getFilters
        } yield Filter.isIncluded(filters)(path)

    val pathToLocalFile: Path => TaskR[Config, LocalFiles] =
      path => localFile(hashService)(path)

    def loop(path: Path): TaskR[Config, LocalFiles] = {

      def dirPaths(path: Path): TaskR[Config, Stream[Path]] =
        for {
          paths    <- listFiles(path)
          filtered <- includedDirPaths(paths)
        } yield filtered

      def includedDirPaths: Stream[Path] => TaskR[Config, Stream[Path]] =
        paths => {
          for {
            flaggedPaths <- TaskR.foreach(paths)(path =>
              isIncluded(path).map((path, _)))
          } yield
            flaggedPaths.toStream
              .filter({ case (_, included) => included })
              .map({ case (path, _) => path })
        }

      def recurseIntoSubDirectories(path: Path): TaskR[Config, LocalFiles] =
        path.toFile match {
          case f if f.isDirectory => loop(path)
          case _                  => pathToLocalFile(path)
        }

      def recurse(paths: Stream[Path]): TaskR[Config, LocalFiles] =
        for {
          recursed <- ZIO.foreach(paths)(path =>
            recurseIntoSubDirectories(path))
        } yield LocalFiles.reduce(recursed.toStream)

      for {
        paths      <- dirPaths(path)
        localFiles <- recurse(paths)
      } yield localFiles
    }

    loop(source)
  }

  def localFile(
      hashService: HashService
  ): Path => TaskR[Config, LocalFiles] =
    path => {
      val file = path.toFile
      for {
        sources <- getSources
        prefix  <- getPrefix
        hash    <- hashService.hashLocalObject(path)
        localFile = LocalFile(file,
                              sources.forPath(path).toFile,
                              hash,
                              generateKey(sources, prefix)(path))
      } yield
        LocalFiles(localFiles = Stream(localFile),
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
