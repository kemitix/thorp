package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import com.amazonaws.services.s3.model.PutObjectResult
import net.kemitix.s3thorp.domain.{Bucket, Config, LocalFile, RemoteKey}
import net.kemitix.s3thorp.Logging
import software.amazon.awssdk.services.s3.model.{CopyObjectResponse, DeleteObjectResponse, ListObjectsV2Response}

trait S3ClientLogging
  extends Logging {

  def logListObjectsStart(bucket: Bucket,
                          prefix: RemoteKey)
                         (implicit c: Config): ListObjectsV2Response => IO[ListObjectsV2Response] = {
    in => IO {
      log3(s"Fetch S3 Summary: ${bucket.name}:${prefix.key}")
      in
    }
  }

  def logListObjectsFinish(bucket: Bucket,
                           prefix: RemoteKey)
                          (implicit c: Config): ListObjectsV2Response => IO[Unit] = {
    in => IO {
      log2(s"Fetched S3 Summary: ${bucket.name}:${prefix.key}")
    }
  }

  def logUploadStart(localFile: LocalFile,
                     bucket: Bucket)
                    (implicit c: Config): PutObjectResult => IO[PutObjectResult] = {
    in => IO {
      log4(s"Uploading: ${bucket.name}:${localFile.remoteKey.key}")
      in
    }
  }

  def logUploadFinish(localFile: LocalFile,
                      bucket: Bucket)
                     (implicit c: Config): PutObjectResult => IO[Unit] = {
    in =>IO {
      log1(s"Uploaded: ${bucket.name}:${localFile.remoteKey.key}")
    }
  }

  def logCopyStart(bucket: Bucket,
                   sourceKey: RemoteKey,
                   targetKey: RemoteKey)
                  (implicit c: Config): CopyObjectResponse => IO[CopyObjectResponse] = {
    in => IO {
      log4(s"Copy: ${bucket.name}:${sourceKey.key} => ${targetKey.key}")
      in
    }
  }

  def logCopyFinish(bucket: Bucket,
                    sourceKey: RemoteKey,
                    targetKey: RemoteKey)
                   (implicit c: Config): CopyObjectResponse => IO[Unit] = {
    in => IO {
      log3(s"Copied: ${bucket.name}:${sourceKey.key} => ${targetKey.key}")
    }
  }

  def logDeleteStart(bucket: Bucket,
                     remoteKey: RemoteKey)
                    (implicit c: Config): DeleteObjectResponse => IO[DeleteObjectResponse] = {
    in => IO {
      log4(s"Delete: ${bucket.name}:${remoteKey.key}")
      in
    }
  }

  def logDeleteFinish(bucket: Bucket,
                      remoteKey: RemoteKey)
                     (implicit c: Config): DeleteObjectResponse => IO[Unit] = {
    in => IO {
      log3(s"Deleted: ${bucket.name}:${remoteKey.key}")
    }
  }

}
