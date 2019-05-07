package net.kemitix.s3thorp

import net.kemitix.s3thorp.Sync.{Hash, LastModified}

trait S3Client {

  def objectHead(bucket: String, key: String): (Hash, LastModified)

}
