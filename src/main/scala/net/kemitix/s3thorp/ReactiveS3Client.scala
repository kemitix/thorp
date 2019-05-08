package net.kemitix.s3thorp

import cats.effect.IO
import com.github.j5ik2o.reactive.aws.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.{S3AsyncClient => JavaS3AsyncClient}

class ReactiveS3Client extends S3Client {

  val s3Client = S3AsyncClient(JavaS3AsyncClient.create())

  override def objectHead(bucket: String, key: String) = {
    IO.fromFuture(IO(
      s3Client.headObject(HeadObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build()))).
      map(r => (r.eTag(), r.lastModified()))
  }
}
