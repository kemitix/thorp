package net.kemitix.s3thorp

import cats.effect.IO
import net.kemitix.s3thorp.awssdk.{S3ObjectsData, S3Client}

trait DummyS3Client extends S3Client {

  override def upload(localFile: LocalFile,
                      bucket: Bucket,
                      tryCount: Int
                     )(implicit c: Config): IO[UploadS3Action] = ???

  override def copy(bucket: Bucket,
                    sourceKey: RemoteKey,
                    hash: MD5Hash,
                    targetKey: RemoteKey
                   )(implicit c: Config): IO[CopyS3Action] = ???

  override def delete(bucket: Bucket,
                      remoteKey: RemoteKey
                     )(implicit c: Config): IO[DeleteS3Action] = ???

  override def listObjects(bucket: Bucket,
                           prefix: RemoteKey
                          )(implicit c: Config): IO[S3ObjectsData] = ???

}
