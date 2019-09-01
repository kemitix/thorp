package net.kemitix.thorp.core

import java.io.File
import java.nio.file.Path

import net.kemitix.thorp.config.Config
import net.kemitix.thorp.core.hasher.Hasher
import net.kemitix.thorp.domain.Sources
import net.kemitix.thorp.filesystem.FileSystem
import zio.{Task, RIO, ZIO}

object LocalFileStream {

  def findFiles(
      source: Path
  ): RIO[Config with FileSystem with Hasher, LocalFiles] = {

    def recurseIntoSubDirectories(
        path: Path): RIO[Config with FileSystem with Hasher, LocalFiles] =
      path.toFile match {
        case f if f.isDirectory => loop(path)
        case _                  => localFile(path)
      }

    def recurse(paths: LazyList[Path])
      : RIO[Config with FileSystem with Hasher, LocalFiles] =
      for {
        recursed <- ZIO.foreach(paths)(path => recurseIntoSubDirectories(path))
      } yield LocalFiles.reduce(LazyList.from(recursed))

    def loop(path: Path): RIO[Config with FileSystem with Hasher, LocalFiles] =
      dirPaths(path) >>= recurse

    loop(source)
  }

  private def dirPaths(path: Path) =
    listFiles(path) >>= includedDirPaths

  private def includedDirPaths(paths: LazyList[Path]) =
    for {
      flaggedPaths <- RIO.foreach(paths)(path =>
        isIncluded(path).map((path, _)))
    } yield
      LazyList
        .from(flaggedPaths)
        .filter({ case (_, included) => included })
        .map({ case (path, _) => path })

  private def localFile(path: Path) =
    for {
      sources <- Config.sources
      prefix  <- Config.prefix
      source  <- Sources.forPath(path)(sources)
      hash    <- Hasher.hashObject(path)
      localFile <- LocalFileValidator.validate(path,
                                               source.toFile,
                                               hash,
                                               sources,
                                               prefix)
    } yield LocalFiles.one(localFile)

  private def listFiles(path: Path) =
    for {
      files <- Task(path.toFile.listFiles)
      _     <- filesMustExist(path, files)
    } yield LazyList.from(files.toIndexedSeq).map(_.toPath)

  private def filesMustExist(path: Path, files: Array[File]) =
    Task {
      Option(files)
        .map(_ => ())
        .getOrElse(new IllegalArgumentException(s"Directory not found $path"))
    }

  private def isIncluded(path: Path) =
    for {
      filters <- Config.filters
    } yield Filters.isIncluded(path)(filters)

}
