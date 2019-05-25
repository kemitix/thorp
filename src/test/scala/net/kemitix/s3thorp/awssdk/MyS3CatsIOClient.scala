package net.kemitix.s3thorp.awssdk

import com.github.j5ik2o.reactive.aws.s3.S3AsyncClient
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import software.amazon.awssdk.services.s3

trait MyS3CatsIOClient extends S3CatsIOClient {
  override val underlying: S3AsyncClient = new S3AsyncClient {
    override val underlying: s3.S3AsyncClient = new s3.S3AsyncClient {
      override def serviceName(): String = "fake-s3-client"

      override def close(): Unit = ()
    }
  }
}
