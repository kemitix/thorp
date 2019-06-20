package net.kemitix.thorp.aws.lib

import cats.effect.IO
import net.kemitix.thorp.domain.{Bucket, Logger, RemoteKey}

object S3ClientLogging {

  def logListObjectsStart(bucket: Bucket,
                          prefix: RemoteKey)
                         (implicit logger: Logger): IO[Unit] =
    logger.info(s"Fetch S3 Summary: ${bucket.name}:${prefix.key}")

  def logListObjectsFinish(bucket: Bucket,
                           prefix: RemoteKey)
                          (implicit logger: Logger): IO[Unit] =
    logger.info(s"Fetched S3 Summary: ${bucket.name}:${prefix.key}")

  def logCopyStart(bucket: Bucket,
                   sourceKey: RemoteKey,
                   targetKey: RemoteKey)
                  (implicit logger: Logger): IO[Unit] =
    logger.info(s"Copy: ${bucket.name}:${sourceKey.key} => ${targetKey.key}")

  def logCopyFinish(bucket: Bucket,
                    sourceKey: RemoteKey,
                    targetKey: RemoteKey)
                   (implicit logger: Logger): IO[Unit] =
    logger.info(s"Copied: ${bucket.name}:${sourceKey.key} => ${targetKey.key}")

  def logDeleteStart(bucket: Bucket,
                     remoteKey: RemoteKey)
                    (implicit logger: Logger): IO[Unit] =
      logger.info(s"Delete: ${bucket.name}:${remoteKey.key}")

  def logDeleteFinish(bucket: Bucket,
                      remoteKey: RemoteKey)
                     (implicit logger: Logger): IO[Unit] =
      logger.info(s"Deleted: ${bucket.name}:${remoteKey.key}")

}
