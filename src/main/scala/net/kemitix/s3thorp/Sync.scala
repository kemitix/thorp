package net.kemitix.s3thorp

import java.io.File

import cats.effect.IO
import cats.implicits._
import net.kemitix.s3thorp.awssdk.{S3Client, UploadProgressListener}
import net.kemitix.s3thorp.domain.{Bucket, Config, LocalFile, MD5Hash, RemoteKey, S3ObjectsData}

class Sync(s3Client: S3Client, md5HashGenerator: File => MD5Hash)
  extends LocalFileStream(md5HashGenerator)
    with S3MetaDataEnricher
    with ActionGenerator
    with ActionSubmitter {

  def run(info: Int => String => Unit,
          warn: String => Unit,
          error: String => Unit)
         (implicit c: Config): IO[Unit] = {
    SyncLogging.logRunStart(info)
    listObjects(c.bucket, c.prefix)
      .map { implicit s3ObjectsData => {
        SyncLogging.logFileScan(info)
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
          if key.isMissingLocally(c.source, c.prefix)
          ioDelAction <- submitAction(ToDelete(key))
        } yield ioDelAction).toStream.sequence
        val delList = delActions.unsafeRunSync.toList
        SyncLogging.logRunFinished(list ++ delList, info)
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
