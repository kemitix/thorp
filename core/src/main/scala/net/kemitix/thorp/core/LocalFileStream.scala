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

    def recurseIntoSubDirectories(path: Path): TaskR[Config, LocalFiles] =
      path.toFile match {
        case f if f.isDirectory => loop(path)
        case _                  => pathToLocalFile(hashService)(path)
      }

    def recurse(paths: Stream[Path]): TaskR[Config, LocalFiles] =
      for {
        recursed <- ZIO.foreach(paths)(path => recurseIntoSubDirectories(path))
      } yield LocalFiles.reduce(recursed.toStream)

    def loop(path: Path): TaskR[Config, LocalFiles] = {

      for {
        paths      <- dirPaths(path)
        localFiles <- recurse(paths)
      } yield localFiles
    }

    loop(source)
  }

  private def dirPaths(path: Path) =
    for {
      paths    <- listFiles(path)
      filtered <- includedDirPaths(paths)
    } yield filtered

  private def includedDirPaths(paths: Stream[Path]) =
    for {
      flaggedPaths <- TaskR.foreach(paths)(path =>
        isIncluded(path).map((path, _)))
    } yield
      flaggedPaths.toStream
        .filter({ case (_, included) => included })
        .map({ case (path, _) => path })

  private def localFile(hashService: HashService)(path: Path) = {
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

  private def listFiles(path: Path) =
    for {
      files <- Task(path.toFile.listFiles)
      _ <- Task.when(files == null)(
        Task.fail(new IllegalArgumentException(s"Directory not found $path")))
    } yield Stream(files: _*).map(_.toPath)

  private def isIncluded(path: Path) =
    for {
      filters <- getFilters
    } yield Filter.isIncluded(filters)(path)

  private def pathToLocalFile(hashService: HashService)(path: Path) =
    localFile(hashService)(path)

}
