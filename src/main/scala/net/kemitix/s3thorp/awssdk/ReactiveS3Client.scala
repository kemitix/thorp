package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import net.kemitix.s3thorp.Sync.{Bucket, LocalFile, RemoteKey}
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.model.{HeadObjectRequest, PutObjectRequest}

private class ReactiveS3Client
  extends S3Client
    with CatsIOS3Client {

  override def objectHead(bucket: Bucket, remoteKey: RemoteKey) = {
    val request = HeadObjectRequest.builder().bucket(bucket).key(remoteKey).build()
    s3Client.headObject(request).attempt.map {
      case Right(response) => Some((response.eTag(), response.lastModified()))
      case Left(_) =>None
    }
  }

  override def upload(localFile: LocalFile, bucket: Bucket, remoteKey: RemoteKey): IO[Unit] = {
    val request = PutObjectRequest.builder().bucket(bucket).key(remoteKey).build()
    val body = AsyncRequestBody.fromFile(localFile)
    for {
      _ <- s3Client.putObject(request, body)
    } yield ()
  }
}
