package net.kemitix.s3thorp

import cats.effect.IO
import cats.implicits._
import net.kemitix.s3thorp.awssdk.S3Client

class Sync(s3Client: S3Client)
  extends LocalFileStream
    with S3MetaDataEnricher
    with ActionGenerator
    with ActionSubmitter
    with SyncLogging {

  def run(implicit c: Config): IO[Unit] = {
    log1(s"Bucket: ${c.bucket.name}, Prefix: ${c.prefix.key}, Source: ${c.source}")
    listObjects(c.bucket, c.prefix)
      .map { implicit s3ObjectsData => {
        val actions = (for {
          file <- findFiles(c.source)
          meta = getMetadata(file)
          action <- createActions(meta)
          ioS3Action = submitAction(action)
        } yield ioS3Action).sequence
        val sorted = sort(actions)
        log(sorted.unsafeRunSync)
      }}
  }

  private def sort(ioActions: IO[Stream[S3Action]]) =
    ioActions.flatMap { actions => IO { actions.sorted } }

  override def upload(localFile: LocalFile,
                      bucket: Bucket) =
    s3Client.upload(localFile, bucket)

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
