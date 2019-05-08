package net.kemitix.s3thorp

import com.github.j5ik2o.reactive.aws.s3.S3AsyncClient
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.{S3AsyncClient => JavaS3AsyncClient}

class ReactiveS3Client extends S3Client {

  private val s3Client = S3CatsIOClient(S3AsyncClient(JavaS3AsyncClient.create))

  override def objectHead(bucket: String, key: String) = {
    val request = HeadObjectRequest.builder()
      .bucket(bucket)
      .key(key)
      .build()
    s3Client.headObject(request).
      map(r => (r.eTag(), r.lastModified()))
  }
}
