package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import net.kemitix.s3thorp.Sync._
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.model.{HeadObjectRequest, PutObjectRequest}

private class ThropS3Client(s3Client: S3CatsIOClient) extends S3Client {

  def objectHead(bucket: Bucket, remoteKey: RemoteKey) = {
    val request = HeadObjectRequest.builder().bucket(bucket).key(remoteKey).build()
    s3Client.headObject(request).attempt.map {
      case Right(response) => Some((response.eTag(), response.lastModified()))
      case Left(_) => None
    }
  }

  def upload(localFile: LocalFile, bucket: Bucket, remoteKey: RemoteKey): IO[Either[Throwable, MD5Hash]] = {
    val request = PutObjectRequest.builder().bucket(bucket).key(remoteKey).build()
    val body = AsyncRequestBody.fromFile(localFile)
    s3Client.putObject(request, body).map{response => Right(response.eTag())}
  }

}
