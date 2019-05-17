package net.kemitix.s3thorp

import java.io.File
import java.time.Instant

import cats.effect._
import net.kemitix.s3thorp.Sync.{LocalFile, MD5Hash}
import net.kemitix.s3thorp.awssdk.S3Client

class Sync(s3Client: S3Client)
  extends LocalFileStream
    with S3MetaDataEnricher
    with UploadSelectionFilter
    with S3Uploader
    with Logging {

  def run(c: Config): IO[Unit] = {
    implicit val config: Config = c
    log1(s"Bucket: ${c.bucket}, Prefix: ${c.prefix}, Source: ${c.source}")
    listObjects(c.bucket, c.prefix).map { hashLookup => {
      val stream: Stream[(File, IO[Either[Throwable, MD5Hash]])] = streamDirectoryPaths(c.source).map(
        enrichWithS3MetaData(c)(hashLookup)).flatMap(
        uploadRequiredFilter(c)).map(
        performUpload(c))
      val count: Int = stream.foldLeft(0)((a: Int, io) => {
        io._2.unsafeRunSync
        log1(s"-     Done: ${io._1}")
        a + 1
      })
      log1(s"Uploaded $count files")
    }}
  }

  override def upload(localFile: LocalFile,
                      bucket: Bucket,
                      remoteKey: RemoteKey) =
    s3Client.upload(localFile, bucket, remoteKey)

  override def copy(bucket: Bucket,
                    sourceKey: RemoteKey,
                    hash: MD5Hash,
                    targetKey: RemoteKey) =
    s3Client.copy(bucket, sourceKey, hash, targetKey)

  override def delete(bucket: Bucket,
                      remoteKey: RemoteKey) =
    s3Client.delete(bucket, remoteKey)

  override def listObjects(bucket: Bucket,
                           prefix: RemoteKey
                          ) =
    s3Client.listObjects(bucket, prefix)
}

object Sync {

  type LocalFile = File // the file or directory
  type MD5Hash = String // an MD5 hash
  type LastModified = Instant // or scala equivalent

}
