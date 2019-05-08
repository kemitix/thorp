package net.kemitix.s3thorp

import java.io.File
import java.time.Instant

import cats.effect._
import fs2.Stream
import net.kemitix.s3thorp.Main.putStrLn

import scala.concurrent.Promise

class Sync(s3Client: S3Client) extends LocalFileStream with S3MetaDataEnricher {

  override def objectHead(bucket: String, key: String)=
    s3Client.objectHead(bucket, key)

  def run(c: Config): IO[Unit] = for {
    _ <- putStrLn(s"Bucket: ${c.bucket}, Prefix: ${c.prefix}, Source: ${c.source}")
    _ <- {
      streamDirectoryPaths(c.source).flatMap(
        enrichWithS3MetaData(c)).flatMap(
        uploadRequiredFilter).flatMap(
        performUpload).compile.drain
    }
  } yield ()

  private def uploadRequiredFilter: S3MetaData => Stream[IO, File] = s3Metadata => Stream.eval(for {
    _ <- putStrLn(s"upload required: ${s3Metadata.localFile}")
    //md5File(localFile)
    //filter(localHash => options.force || localHash != metadataHash)
  } yield s3Metadata.localFile)

  private def performUpload: File => Stream[IO, Promise[Unit]] =
    file => Stream.eval(for {
      _ <- putStrLn(s"upload: $file")
      // upload
      p = Promise[Unit]()
    } yield p)

}

object Sync {

  type Bucket = String // the S3 bucket name
  type LocalFile = File // the file or directory
  type RemotePath = String // path within an S3 bucket
  type Hash = String // an MD5 hash
  type LastModified = Instant // or scala equivalent

}
