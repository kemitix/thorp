package net.kemitix.s3thorp

import java.io.File

import cats.effect.{IO, _}
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
      val s3Actions: Stream[IO[S3Action]] = for {
        file <- streamDirectoryPaths(c.source)
        meta = enrichWithS3MetaData(c)(hashLookup)(file)
        toUp <- uploadRequiredFilter(c)(meta)
        s3Action = enquedUpload(c)(toUp)
      } yield s3Action
      val count = s3Actions.foldLeft(0)((a: Int, ioS3Action: IO[S3Action]) => {
        val s3Action = ioS3Action.unsafeRunSync
        log1(s"-     Done: ${s3Action.remoteKey.key}")
        a + 1
      })
      log1(s"Uploaded $count files")
    }}
  }

  override def upload(localFile: File,
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
