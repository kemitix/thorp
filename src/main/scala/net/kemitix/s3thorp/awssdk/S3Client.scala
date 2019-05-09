package net.kemitix.s3thorp.awssdk

import cats.effect.IO
import net.kemitix.s3thorp.Sync.{Hash, LastModified}

trait S3Client {

  def objectHead(bucket: String, key: String): IO[Option[(Hash, LastModified)]]

}

object S3Client {

  val defaultClient: S3Client = new ReactiveS3Client

}