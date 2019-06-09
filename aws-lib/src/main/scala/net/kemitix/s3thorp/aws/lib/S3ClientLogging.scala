package net.kemitix.s3thorp.aws.lib

import cats.effect.IO
import com.amazonaws.services.s3.model.{CopyObjectResult, DeleteObjectsResult, ListObjectsV2Result, PutObjectResult, S3ObjectSummary}
import net.kemitix.s3thorp.domain.{Bucket, LocalFile, RemoteKey}

object S3ClientLogging {

  def logListObjectsStart(bucket: Bucket,
                          prefix: RemoteKey)
                         (implicit info: Int => String => IO[Unit]): IO[Unit] =
    info(1)(s"Fetch S3 Summary: ${bucket.name}:${prefix.key}")

  def logListObjectsFinish(bucket: Bucket,
                           prefix: RemoteKey)
                          (implicit info: Int => String => IO[Unit]): IO[Unit] =
    info(2)(s"Fetched S3 Summary: ${bucket.name}:${prefix.key}")

  def logUploadStart(localFile: LocalFile,
                     bucket: Bucket)
                    (implicit info: Int => String => IO[Unit]): PutObjectResult => IO[PutObjectResult] =
    in => for {
      _ <- info(1)(s"Uploading: ${bucket.name}:${localFile.remoteKey.key}")
    } yield in

  def logUploadFinish(localFile: LocalFile,
                      bucket: Bucket)
                     (implicit info: Int => String => IO[Unit]): PutObjectResult => IO[Unit] =
    _ => info(2)(s"Uploaded: ${bucket.name}:${localFile.remoteKey.key}")

  def logCopyStart(bucket: Bucket,
                   sourceKey: RemoteKey,
                   targetKey: RemoteKey)
                  (implicit info: Int => String => IO[Unit]): IO[Unit] =
    info(1)(s"Copy: ${bucket.name}:${sourceKey.key} => ${targetKey.key}")

  def logCopyFinish(bucket: Bucket,
                    sourceKey: RemoteKey,
                    targetKey: RemoteKey)
                   (implicit info: Int => String => IO[Unit]): IO[Unit] =
    info(2)(s"Copied: ${bucket.name}:${sourceKey.key} => ${targetKey.key}")

  def logDeleteStart(bucket: Bucket,
                     remoteKey: RemoteKey)
                    (implicit info: Int => String => IO[Unit]): IO[Unit] =
      info(1)(s"Delete: ${bucket.name}:${remoteKey.key}")

  def logDeleteFinish(bucket: Bucket,
                      remoteKey: RemoteKey)
                     (implicit info: Int => String => IO[Unit]): IO[Unit] =
      info(2)(s"Deleted: ${bucket.name}:${remoteKey.key}")

}
