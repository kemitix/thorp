package net.kemitix.s3thorp

import cats.effect.IO
import net.kemitix.s3thorp.Sync.{Hash, LastModified}

trait S3Client {

  def objectHead(bucket: String, key: String): IO[(Hash, LastModified)]

}
