package net.kemitix.s3thorp.awssdk

import com.github.j5ik2o.reactive.aws.s3.S3AsyncClient
import com.github.j5ik2o.reactive.aws.s3.cats.S3CatsIOClient
import software.amazon.awssdk.services.s3.{S3AsyncClient => JavaS3AsyncClient}

trait JavaClientWrapper extends S3CatsIOClient {

  override val underlying: S3AsyncClient =
    S3AsyncClient(JavaS3AsyncClient.create)

}
