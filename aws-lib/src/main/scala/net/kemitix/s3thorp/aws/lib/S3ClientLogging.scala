package net.kemitix.s3thorp.aws.lib

import cats.effect.IO
import com.amazonaws.services.s3.model.{CopyObjectResult, DeleteObjectsResult, ListObjectsV2Result, PutObjectResult}
import net.kemitix.s3thorp.domain.{Bucket, LocalFile, RemoteKey}

object S3ClientLogging {

  def logListObjectsStart(bucket: Bucket,
                          prefix: RemoteKey)
                         (implicit info: Int => String => Unit): ListObjectsV2Result => IO[ListObjectsV2Result] = {
    in => IO {
      info(3)(s"Fetch S3 Summary: ${bucket.name}:${prefix.key}")
      in
    }
  }

  def logListObjectsFinish(bucket: Bucket,
                           prefix: RemoteKey)
                          (implicit info: Int => String => Unit): ListObjectsV2Result => IO[Unit] = {
    in => IO {
      info(2)(s"Fetched S3 Summary: ${bucket.name}:${prefix.key}")
    }
  }

  def logUploadStart(localFile: LocalFile,
                     bucket: Bucket)
                    (implicit info: Int => String => Unit): PutObjectResult => IO[PutObjectResult] = {
    in => IO {
      info(4)(s"Uploading: ${bucket.name}:${localFile.remoteKey.key}")
      in
    }
  }

  def logUploadFinish(localFile: LocalFile,
                      bucket: Bucket)
                     (implicit info: Int => String => Unit): PutObjectResult => IO[Unit] = {
    in =>IO {
      info(1)(s"Uploaded: ${bucket.name}:${localFile.remoteKey.key}")
    }
  }

  def logCopyStart(bucket: Bucket,
                   sourceKey: RemoteKey,
                   targetKey: RemoteKey)
                  (implicit info: Int => String => Unit): CopyObjectResult => IO[CopyObjectResult] = {
    in => IO {
      info(4)(s"Copy: ${bucket.name}:${sourceKey.key} => ${targetKey.key}")
      in
    }
  }

  def logCopyFinish(bucket: Bucket,
                    sourceKey: RemoteKey,
                    targetKey: RemoteKey)
                   (implicit info: Int => String => Unit): CopyObjectResult => IO[Unit] = {
    in => IO {
      info(3)(s"Copied: ${bucket.name}:${sourceKey.key} => ${targetKey.key}")
    }
  }

  def logDeleteStart(bucket: Bucket,
                     remoteKey: RemoteKey)
                    (implicit info: Int => String => Unit): IO[Unit] =
    IO{info(4)(s"Delete: ${bucket.name}:${remoteKey.key}")}

  def logDeleteFinish(bucket: Bucket,
                      remoteKey: RemoteKey)
                     (implicit info: Int => String => Unit): IO[Unit] =
    IO {info(3)(s"Deleted: ${bucket.name}:${remoteKey.key}")}

}
