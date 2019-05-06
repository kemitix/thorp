package net.kemitix.s3thorp

import java.nio.file.{DirectoryStream, Files, Path, Paths}

import scala.collection.JavaConverters._
import fs2.Stream
import cats.effect._
import Main.putStrLn

object Sync {
  def apply(c: Config): IO[Unit] = for {
    _ <- putStrLn(s"Bucket: ${c.bucket}, Prefix: ${c.prefix}, Source: ${c.source}")
    _ <- {

      // a stream of files in the source directory
      val pathStream: Stream[IO, Path] = streamDirectoryPaths(Paths.get(c.source))


      IO.unit
    }
  } yield ()

  private def streamDirectoryPaths(path: Path): Stream[IO, Path] = {

    def acquire: Path => IO[DirectoryStream[Path]] =
      p => IO(Files.newDirectoryStream(p))

    def release: DirectoryStream[Path] => IO[Unit] =
      ds => IO(ds.close())

    def openDirectory: Path => Stream[IO, Path] =
      p => Stream.bracket(acquire(p))(release).
        map(ds => ds.iterator()).
        map(ji => ji.asScala).
        flatMap(it => Stream.fromIterator[IO, Path](it))

    def recurseIntoSubDirectories: Path => Stream[IO, Path] =
      p =>
        if (p.toFile.isDirectory) streamDirectoryPaths(p)
        else Stream(p)

    Stream.eval(IO(path)).
      flatMap(openDirectory).
      flatMap(recurseIntoSubDirectories)
  }
}
