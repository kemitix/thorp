package net.kemitix.s3thorp

import net.kemitix.s3thorp.Sync.{Bucket, LocalFile}

case class Config(bucket: Bucket = "",
                  prefix: String = "",
                  source: LocalFile
               ) {

}
