package net.kemitix.s3thorp

import java.io.File
import java.time.Instant

import cats.effect._
import net.kemitix.s3thorp.Main.putStrLn
import net.kemitix.s3thorp.Sync.{Bucket, LocalFile, RemoteKey}
import net.kemitix.s3thorp.awssdk.S3Client

class Sync(s3Client: S3Client)
  extends LocalFileStream
    with S3MetaDataEnricher
    with UploadSelectionFilter
    with S3Uploader {

  override def objectHead(bucket: Bucket, remoteKey: RemoteKey)=
    s3Client.objectHead(bucket, remoteKey)

  override def upload(localFile: LocalFile, bucket: Bucket, remoteKey: RemoteKey): IO[Unit] =
    s3Client.upload(localFile, bucket, remoteKey)

  def run(c: Config): IO[Unit] = for {
    _ <- putStrLn(s"Bucket: ${c.bucket}, Prefix: ${c.prefix}, Source: ${c.source}")
    _ <- {
      streamDirectoryPaths(c.source).flatMap(
        enrichWithS3MetaData(c)).flatMap(
        uploadRequiredFilter(c)).flatMap(
        performUpload(c)).compile.drain
    }
  } yield ()

}

object Sync {

  type Bucket = String // the S3 bucket name
  type LocalFile = File // the file or directory
  type RemoteKey = String // path within an S3 bucket
  type MD5Hash = String // an MD5 hash
  type LastModified = Instant // or scala equivalent

}
