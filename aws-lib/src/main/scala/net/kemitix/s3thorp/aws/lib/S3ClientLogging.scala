package net.kemitix.s3thorp.aws.lib

import cats.effect.IO
import com.amazonaws.services.s3.model.PutObjectResult
import net.kemitix.s3thorp.domain.{Bucket, LocalFile, RemoteKey}
import software.amazon.awssdk.services.s3.model.{CopyObjectResponse, DeleteObjectResponse, ListObjectsV2Response}

object S3ClientLogging {

  def logListObjectsStart(bucket: Bucket,
                          prefix: RemoteKey)
                         (implicit info: Int => String => Unit): ListObjectsV2Response => IO[ListObjectsV2Response] = {
    in => IO {
      info(3)(s"Fetch S3 Summary: ${bucket.name}:${prefix.key}")
      in
    }
  }

  def logListObjectsFinish(bucket: Bucket,
                           prefix: RemoteKey)
                          (implicit info: Int => String => Unit): ListObjectsV2Response => IO[Unit] = {
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
                  (implicit info: Int => String => Unit): CopyObjectResponse => IO[CopyObjectResponse] = {
    in => IO {
      info(4)(s"Copy: ${bucket.name}:${sourceKey.key} => ${targetKey.key}")
      in
    }
  }

  def logCopyFinish(bucket: Bucket,
                    sourceKey: RemoteKey,
                    targetKey: RemoteKey)
                   (implicit info: Int => String => Unit): CopyObjectResponse => IO[Unit] = {
    in => IO {
      info(3)(s"Copied: ${bucket.name}:${sourceKey.key} => ${targetKey.key}")
    }
  }

  def logDeleteStart(bucket: Bucket,
                     remoteKey: RemoteKey)
                    (implicit info: Int => String => Unit): DeleteObjectResponse => IO[DeleteObjectResponse] = {
    in => IO {
      info(4)(s"Delete: ${bucket.name}:${remoteKey.key}")
      in
    }
  }

  def logDeleteFinish(bucket: Bucket,
                      remoteKey: RemoteKey)
                     (implicit info: Int => String => Unit): DeleteObjectResponse => IO[Unit] = {
    in => IO {
      info(3)(s"Deleted: ${bucket.name}:${remoteKey.key}")
    }
  }

}
