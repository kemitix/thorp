package net.kemitix.thorp.core

import cats.effect.IO
import net.kemitix.thorp.core.Action.{DoNothing, ToCopy, ToDelete, ToUpload}
import net.kemitix.thorp.domain.{Config, Logger, StorageQueueEvent, UploadEventListener}
import net.kemitix.thorp.domain.StorageQueueEvent.DoNothingQueueEvent
import net.kemitix.thorp.storage.api.StorageService

trait ActionSubmitter {

  def submitAction(storageService: StorageService,
                   action: Action)
                  (implicit logger: Logger): Stream[IO[StorageQueueEvent]] = {
    Stream(
      action match {
        case ToUpload(bucket, localFile) =>
          for {
            _ <- logger.info(s"    Upload: ${localFile.relative}")
            uploadEventListener = new UploadEventListener(localFile)
            event <- storageService.upload(localFile, bucket, uploadEventListener, 1)
          } yield event
        case ToCopy(bucket, sourceKey, hash, targetKey) =>
          for {
            _ <- logger.info(s"      Copy: ${sourceKey.key} => ${targetKey.key}")
            event <- storageService.copy(bucket, sourceKey, hash, targetKey)
          } yield event
        case ToDelete(bucket, remoteKey) =>
          for {
            _ <- logger.info(s"    Delete: ${remoteKey.key}")
            event <- storageService.delete(bucket, remoteKey)
          } yield event
        case DoNothing(_, remoteKey) =>
          IO.pure(DoNothingQueueEvent(remoteKey))
      })
  }
}

object ActionSubmitter extends ActionSubmitter
