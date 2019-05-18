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

  def run(implicit c: Config): IO[Unit] = {
    log1(s"Bucket: ${c.bucket.name}, Prefix: ${c.prefix.key}, Source: ${c.source}")
    listObjects(c.bucket, c.prefix).map { implicit hashLookup => {
      val s3Actions: Stream[IO[S3Action]] = for {
        file <- findFiles(c.source)
        meta = enrichWithS3MetaData(file)
        toUp <- uploadRequiredFilter(meta)
        s3Action = enquedUpload(toUp)
      } yield s3Action
      val counter = s3Actions.foldLeft(Counters())((counters: Counters, ioS3Action: IO[S3Action]) => {
        val s3Action = ioS3Action.unsafeRunSync
        s3Action match {
          case UploadS3Action(_, _) => {
            log1(s"- Uploaded: ${s3Action.remoteKey.key}")
            counters.copy(uploaded = counters.uploaded + 1)
          }
          case _ => counters
        }
      })
      log1(s"Uploaded ${counter.uploaded} files")
      log1(s"Copied   ${counter.copied} files")
      log1(s"Moved    ${counter.moved} files")
      log1(s"Deleted  ${counter.deleted} files")
    }}
  }

  case class Counters(uploaded: Int = 0,
                      deleted: Int = 0,
                      copied: Int = 0,
                      moved: Int = 0)

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
