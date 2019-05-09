package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import com.github.j5ik2o.reactive.aws.s3.S3AsyncClient
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import software.amazon.awssdk.services.s3.model.{HeadObjectRequest, NoSuchKeyException}
import net.kemitix.s3thorp.Sync.{Bucket, LocalFile, RemoteKey}
import software.amazon.awssdk.services.s3.{S3AsyncClient => JavaS3AsyncClient}

private class ReactiveS3Client extends S3Client {

  private val s3Client = S3CatsIOClient(S3AsyncClient(JavaS3AsyncClient.create))

  override def objectHead(bucket: Bucket, remoteKey: RemoteKey) = {
    val request = HeadObjectRequest.builder()
      .bucket(bucket)
      .key(remoteKey)
      .build()
    try {
      for {
        response <- s3Client.headObject(request)
      } yield Some((response.eTag(), response.lastModified()))
    } catch {
      case _: NoSuchKeyException => IO(None)
    }
  }
}
