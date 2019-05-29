package net.kemitix.s3thorp

import cats.effect.IO
import cats.implicits._
import net.kemitix.s3thorp.awssdk.{S3Client, S3ObjectsData, UploadProgressListener}

class Sync(s3Client: S3Client)
  extends LocalFileStream
    with S3MetaDataEnricher
    with ActionGenerator
    with ActionSubmitter
    with SyncLogging {

  def run(implicit c: Config): IO[Unit] = {
    logRunStart
    listObjects(c.bucket, c.prefix)
      .map { implicit s3ObjectsData => {
        logFileScan
        val actions = for {
          file <- findFiles(c.source)
          data <- getMetadata(file)
          action <- createActions(data)
          s3Action <- submitAction(action)
        } yield s3Action
        val sorted = sort(actions.sequence)
        val list = sorted.unsafeRunSync.toList
        val delActions = (for {
          key <- s3ObjectsData.byKey.keys
          if key.isMissingLocally
          ioDelAction <- submitAction(ToDelete(key))
        } yield ioDelAction).toStream.sequence
        val delList = delActions.unsafeRunSync.toList
        logRunFinished(list ++ delList)
      }}
  }

  private def sort(ioActions: IO[Stream[S3Action]]) =
    ioActions.flatMap { actions => IO { actions.sorted } }

  override def upload(localFile: LocalFile,
                      bucket: Bucket,
                      progressListener: UploadProgressListener,
                      tryCount: Int)
                     (implicit c: Config): IO[S3Action] =
    s3Client.upload(localFile, bucket, progressListener, tryCount)

  override def copy(bucket: Bucket,
                    sourceKey: RemoteKey,
                    hash: MD5Hash,
                    targetKey: RemoteKey)(implicit c: Config): IO[CopyS3Action] =
    s3Client.copy(bucket, sourceKey, hash, targetKey)

  override def delete(bucket: Bucket,
                      remoteKey: RemoteKey)(implicit c: Config): IO[DeleteS3Action] =
    s3Client.delete(bucket, remoteKey)

  override def listObjects(bucket: Bucket,
                           prefix: RemoteKey
                          )(implicit c: Config): IO[S3ObjectsData] =
    s3Client.listObjects(bucket, prefix)
}
