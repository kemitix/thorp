package net.kemitix.s3thorp

import java.nio.file.{DirectoryStream, Files, Path, Paths}

import cats.effect._
import fs2.Stream
import net.kemitix.s3thorp.Main.putStrLn

import scala.collection.JavaConverters._
import scala.concurrent.Promise

object Sync {
  def apply(c: Config): IO[Unit] = for {
    _ <- putStrLn(s"Bucket: ${c.bucket}, Prefix: ${c.prefix}, Source: ${c.source}")
    _ <- {
      streamDirectoryPaths(Paths.get(c.source)).flatMap(
        enrichWithS3MetaData).flatMap(
        uploadRequiredFilter).flatMap(
        performUpload).compile.drain
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

  case class S3MetaData()

  private def enrichWithS3MetaData: Path => Stream[IO, S3MetaData] = path => {
    // HEAD(bucket, prefix, relative(path))
    // create blank S3MetaData records (sealed trait?)
    }

  private def uploadRequiredFilter: S3MetaData => Stream[IO, Path] =
    s3Metadata => {
      //md5File(localFile)
      //filter(localHash => options.force || localHash != metadataHash)
    }

  private def performUpload: Path => Stream[IO, Promise[Unit]] =
    path => {
      // upload
      IO.unit
    }
}
