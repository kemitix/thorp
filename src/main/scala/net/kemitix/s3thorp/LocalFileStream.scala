package net.kemitix.s3thorp

import java.nio.file.{DirectoryStream, Files, Path}
import fs2.Stream
import scala.collection.JavaConverters._
import cats.effect.IO

trait LocalFileStream {

  def streamDirectoryPaths(path: Path): Stream[IO, Path] =
  {
    Stream.eval(IO(path)).
      flatMap(openDirectory).
      flatMap(recurseIntoSubDirectories)
  }

  private def acquire: Path => IO[DirectoryStream[Path]] =
    p => IO(Files.newDirectoryStream(p))

  private def release: DirectoryStream[Path] => IO[Unit] =
    ds => IO(ds.close())

  private def openDirectory: Path => Stream[IO, Path] =
    p => Stream.bracket(acquire(p))(release).
      map(ds => ds.iterator()).
      map(ji => ji.asScala).
      flatMap(it => Stream.fromIterator[IO, Path](it))

  private def recurseIntoSubDirectories: Path => Stream[IO, Path] =
    p =>
      if (p.toFile.isDirectory) streamDirectoryPaths(p)
      else Stream(p)


}
