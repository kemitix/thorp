package net.kemitix.s3thorp

import java.nio.file.{Path, Paths}
import java.time.Instant

import cats.effect._
import fs2.Stream
import net.kemitix.s3thorp.Main.putStrLn

import scala.collection.JavaConverters._
import scala.concurrent.Promise

object Sync extends LocalFileStream {
  def apply(c: Config): IO[Unit] = for {
    _ <- putStrLn(s"Bucket: ${c.bucket}, Prefix: ${c.prefix}, Source: ${c.source}")
    _ <- {
      streamDirectoryPaths(Paths.get(c.source)).flatMap(
        enrichWithS3MetaData).flatMap(
        uploadRequiredFilter).flatMap(
        performUpload).compile.drain
    }
  } yield ()

  type LocalPath = Path
  type RemotePath = String
  type Hash = String // an MD5 hash
  type LastModified = Instant // or scala equivalent

  case class S3MetaData(localPath: LocalPath,
                        remotePath: RemotePath,
                        remoteHash: Hash,
                        remoteLastModified: LastModified)

  private def enrichWithS3MetaData: Path => Stream[IO, S3MetaData] = path => Stream.eval(for {
    _ <- putStrLn(s"enrich: $path")
    // HEAD(bucket, prefix, relative(path))
    // create blank S3MetaData records (sealed trait?)
  } yield S3MetaData(path, "", "", Instant.now()))

  private def uploadRequiredFilter: S3MetaData => Stream[IO, Path] = s3Metadata => Stream.eval(for {
      _ <- putStrLn(s"upload required: ${s3Metadata.localPath}")
      //md5File(localFile)
      //filter(localHash => options.force || localHash != metadataHash)
    } yield s3Metadata.localPath)

  private def performUpload: Path => Stream[IO, Promise[Unit]] = path => Stream.eval(for {
      _ <- putStrLn(s"upload: $path")
      // upload
      p = Promise[Unit]()
    } yield p)
}
