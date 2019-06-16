package net.kemitix.s3thorp.aws.lib

import cats.Monad
import net.kemitix.thorp.domain.{Bucket, Logger, RemoteKey}

object S3ClientLogging {

  def logListObjectsStart[M[_]: Monad](bucket: Bucket,
                          prefix: RemoteKey)
                         (implicit logger: Logger[M]): M[Unit] =
    logger.info(s"Fetch S3 Summary: ${bucket.name}:${prefix.key}")

  def logListObjectsFinish[M[_]: Monad](bucket: Bucket,
                           prefix: RemoteKey)
                          (implicit logger: Logger[M]): M[Unit] =
    logger.info(s"Fetched S3 Summary: ${bucket.name}:${prefix.key}")

  def logCopyStart[M[_]: Monad](bucket: Bucket,
                                sourceKey: RemoteKey,
                                targetKey: RemoteKey)
                               (implicit logger: Logger[M]): M[Unit] =
    logger.info(s"Copy: ${bucket.name}:${sourceKey.key} => ${targetKey.key}")

  def logCopyFinish[M[_]: Monad](bucket: Bucket,
                    sourceKey: RemoteKey,
                    targetKey: RemoteKey)
                   (implicit logger: Logger[M]): M[Unit] =
    logger.info(s"Copied: ${bucket.name}:${sourceKey.key} => ${targetKey.key}")

  def logDeleteStart[M[_]: Monad](bucket: Bucket,
                     remoteKey: RemoteKey)
                    (implicit logger: Logger[M]): M[Unit] =
      logger.info(s"Delete: ${bucket.name}:${remoteKey.key}")

  def logDeleteFinish[M[_]: Monad](bucket: Bucket,
                      remoteKey: RemoteKey)
                     (implicit logger: Logger[M]): M[Unit] =
      logger.info(s"Deleted: ${bucket.name}:${remoteKey.key}")

}
