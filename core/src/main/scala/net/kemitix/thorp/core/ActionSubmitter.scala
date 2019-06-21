package net.kemitix.thorp.core

import cats.effect.IO
import net.kemitix.thorp.core.Action.{DoNothing, ToCopy, ToDelete, ToUpload}
import net.kemitix.thorp.domain.{Config, Logger}
import net.kemitix.thorp.storage.api.S3Action.DoNothingS3Action
import net.kemitix.thorp.storage.api.{S3Action, StorageService, UploadProgressListener}

object ActionSubmitter {

  def submitAction(storageService: StorageService,
                   action: Action)
                  (implicit c: Config,
                   logger: Logger): Stream[IO[S3Action]] = {
    Stream(
      action match {
        case ToUpload(bucket, localFile) =>
          for {
            _ <- logger.info(s"    Upload: ${localFile.relative}")
            progressListener = new UploadProgressListener(localFile)
            action <- storageService.upload(localFile, bucket, progressListener, 1)
          } yield action
        case ToCopy(bucket, sourceKey, hash, targetKey) =>
          for {
            _ <- logger.info(s"      Copy: ${sourceKey.key} => ${targetKey.key}")
            action <- storageService.copy(bucket, sourceKey, hash, targetKey)
          } yield action
        case ToDelete(bucket, remoteKey) =>
          for {
            _ <- logger.info(s"    Delete: ${remoteKey.key}")
            action <- storageService.delete(bucket, remoteKey)
          } yield action
        case DoNothing(bucket, remoteKey) =>
          IO.pure(DoNothingS3Action(remoteKey))
      })
  }
}
