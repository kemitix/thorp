package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import net.kemitix.s3thorp.{Bucket, Config, LocalFile, Logging, RemoteKey}
import software.amazon.awssdk.services.s3.model.{CopyObjectResponse, DeleteObjectResponse, ListObjectsV2Response, PutObjectResponse}

trait S3ClientLogging
  extends Logging {

  def logListObjectsStart(bucket: Bucket,
                          prefix: RemoteKey)
                         (implicit c: Config): ListObjectsV2Response => IO[ListObjectsV2Response] = {
    in => IO {
      log3(s"listObjects:start:${bucket.name}:${prefix.key}")
      in
    }
  }

  def logListObjectsFinish(bucket: Bucket,
                           prefix: RemoteKey)
                          (implicit c: Config): ListObjectsV2Response => IO[Unit] = {
    in => IO {
      log2(s"listObjects:finish:${bucket.name}:${prefix.key}")
    }
  }

  def logUploadStart(localFile: LocalFile,
                     bucket: Bucket)
                    (implicit c: Config): PutObjectResponse => IO[PutObjectResponse] = {
    in => IO {
      log4(s"upload:start:${localFile.file.length}:${bucket.name}:${localFile.remoteKey}")
      in
    }
  }

  def logUploadFinish(localFile: LocalFile,
                      bucket: Bucket)
                     (implicit c: Config): PutObjectResponse => IO[Unit] = {
    in =>IO {
      log3(s"upload:finish:${localFile.file.length}:${bucket.name}:${localFile.remoteKey}")
    }
  }

  def logCopyStart(bucket: Bucket,
                   sourceKey: RemoteKey,
                   targetKey: RemoteKey)
                  (implicit c: Config): CopyObjectResponse => IO[CopyObjectResponse] = {
    in => IO {
      log4(s"copy:start:${bucket.name}:${sourceKey.key}:${targetKey.key}")
      in
    }
  }

  def logCopyFinish(bucket: Bucket,
                    sourceKey: RemoteKey,
                    targetKey: RemoteKey)
                   (implicit c: Config): CopyObjectResponse => IO[Unit] = {
    in => IO {
      log3(s"copy:finish:${bucket.name}:${sourceKey.key}:${targetKey.key}")
    }
  }

  def logDeleteStart(bucket: Bucket,
                     remoteKey: RemoteKey)
                    (implicit c: Config): DeleteObjectResponse => IO[DeleteObjectResponse] = {
    in => IO {
      log4(s"delete:start:${bucket.name}:${remoteKey.key}")
      in
    }
  }

  def logDeleteFinish(bucket: Bucket,
                      remoteKey: RemoteKey)
                     (implicit c: Config): DeleteObjectResponse => IO[Unit] = {
    in => IO {
      log3(s"delete:finish:${bucket.name}:${remoteKey.key}")
    }
  }

}
