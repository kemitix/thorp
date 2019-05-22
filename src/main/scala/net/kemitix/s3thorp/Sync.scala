package net.kemitix.s3thorp

import java.nio.file.Paths

import cats.effect.IO
import cats.implicits._
import net.kemitix.s3thorp.awssdk.{S3Client, S3ObjectsData}

class Sync(s3Client: S3Client)
  extends LocalFileStream
    with S3MetaDataEnricher
    with ActionGenerator
    with ActionSubmitter
    with SyncLogging {

  def run(implicit c: Config): IO[Unit] = {
    logRunStart(c).unsafeRunSync
    listObjects(c.bucket, c.prefix)
      .map { implicit s3ObjectsData => {
        val actions = (for {
          file <- findFiles(c.source)
          meta = getMetadata(file)
          action <- createActions(meta)
          ioS3Action = submitAction(action)
        } yield ioS3Action).sequence
        val sorted = sort(actions)
        val list = sorted.unsafeRunSync.toList
        val delActions = (for {
          key <- s3ObjectsData.byKey.keys
          if key.isMissingLocally
          ioDelAction = submitAction(ToDelete(key))
        } yield ioDelAction).toStream.sequence
        val delList = delActions.unsafeRunSync.toList
        logRunFinished(list ++ delList).unsafeRunSync
      }}
  }

  private def sort(ioActions: IO[Stream[S3Action]]) =
    ioActions.flatMap { actions => IO { actions.sorted } }

  override def upload(localFile: LocalFile,
                      bucket: Bucket)(implicit c: Config): IO[UploadS3Action] =
    s3Client.upload(localFile, bucket)

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
