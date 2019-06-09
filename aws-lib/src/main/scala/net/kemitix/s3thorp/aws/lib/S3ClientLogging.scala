package net.kemitix.s3thorp.aws.lib

import cats.effect.IO
import com.amazonaws.services.s3.model.{CopyObjectResult, DeleteObjectsResult, ListObjectsV2Result, PutObjectResult, S3ObjectSummary}
import net.kemitix.s3thorp.domain.{Bucket, LocalFile, RemoteKey}

object S3ClientLogging {

  def logListObjectsStart(bucket: Bucket,
                          prefix: RemoteKey)
                         (implicit info: Int => String => IO[Unit]): Stream[S3ObjectSummary] => IO[Stream[S3ObjectSummary]] =
    in => for {
      _ <- info(3)(s"Fetch S3 Summary: ${bucket.name}:${prefix.key}")
    } yield in

  def logListObjectsFinish(bucket: Bucket,
                           prefix: RemoteKey)
                          (implicit info: Int => String => IO[Unit]): Stream[S3ObjectSummary] => IO[Unit] =
    _ => info(2)(s"Fetched S3 Summary: ${bucket.name}:${prefix.key}")

  def logUploadStart(localFile: LocalFile,
                     bucket: Bucket)
                    (implicit info: Int => String => IO[Unit]): PutObjectResult => IO[PutObjectResult] =
    in => for {
      _ <- info(4)(s"Uploading: ${bucket.name}:${localFile.remoteKey.key}")
    } yield in

  def logUploadFinish(localFile: LocalFile,
                      bucket: Bucket)
                     (implicit info: Int => String => IO[Unit]): PutObjectResult => IO[Unit] =
    _ => info(1)(s"Uploaded: ${bucket.name}:${localFile.remoteKey.key}")

  def logCopyStart(bucket: Bucket,
                   sourceKey: RemoteKey,
                   targetKey: RemoteKey)
                  (implicit info: Int => String => IO[Unit]): CopyObjectResult => IO[CopyObjectResult] =
    in => for {
      _ <- info(4)(s"Copy: ${bucket.name}:${sourceKey.key} => ${targetKey.key}")
    } yield in

  def logCopyFinish(bucket: Bucket,
                    sourceKey: RemoteKey,
                    targetKey: RemoteKey)
                   (implicit info: Int => String => IO[Unit]): CopyObjectResult => IO[Unit] =
    _ => info(3)(s"Copied: ${bucket.name}:${sourceKey.key} => ${targetKey.key}")

  def logDeleteStart(bucket: Bucket,
                     remoteKey: RemoteKey)
                    (implicit info: Int => String => IO[Unit]): IO[Unit] =
      info(4)(s"Delete: ${bucket.name}:${remoteKey.key}")

  def logDeleteFinish(bucket: Bucket,
                      remoteKey: RemoteKey)
                     (implicit info: Int => String => IO[Unit]): IO[Unit] =
      info(3)(s"Deleted: ${bucket.name}:${remoteKey.key}")

}
