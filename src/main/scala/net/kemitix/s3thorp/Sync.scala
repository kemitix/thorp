package net.kemitix.s3thorp

import java.io.File
import java.time.Instant

import cats.effect._
import net.kemitix.s3thorp.Sync.{Bucket, LocalFile, MD5Hash, RemoteKey}
import net.kemitix.s3thorp.awssdk.{HashLookup, S3Client}

class Sync(s3Client: S3Client)
  extends LocalFileStream
    with S3MetaDataEnricher
    with UploadSelectionFilter
    with S3Uploader
    with Logging {

  override def upload(localFile: LocalFile, bucket: Bucket, remoteKey: RemoteKey) =
    s3Client.upload(localFile, bucket, remoteKey)

  def run(c: Config): IO[Unit] = {
    logger.info(s"Bucket: ${c.bucket}, Prefix: ${c.prefix}, Source: ${c.source}")
    s3Client.listObjects(c.bucket, c.prefix).map { hashLookup => {
      val stream: Stream[(File, IO[Either[Throwable, MD5Hash]])] = streamDirectoryPaths(c.source).map(
        enrichWithS3MetaData(c)(hashLookup)).flatMap(
        uploadRequiredFilter(c)).map(
        performUpload(c))
      val count: Int = stream.foldLeft(0)((a: Int, io) => {
        io._2.unsafeRunSync
        logger.info(s"-     Done: ${io._1}")
        a + 1
      })
      logger.info(s"Uploaded $count files")
    }}
  }

  override def listObjects(bucket: Bucket, prefix: RemoteKey): IO[HashLookup] = ???
}

object Sync {

  type Bucket = String // the S3 bucket name
  type LocalFile = File // the file or directory
  type RemoteKey = String // path within an S3 bucket
  type MD5Hash = String // an MD5 hash
  type LastModified = Instant // or scala equivalent

}
