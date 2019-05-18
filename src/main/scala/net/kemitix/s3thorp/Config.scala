package net.kemitix.s3thorp

import java.io.File

case class Config(bucket: Bucket = Bucket(""),
                  prefix: RemoteKey = RemoteKey(""),
                  verbose: Int = 1,
                  source: File
               ) {
  require(source.isDirectory, s"Source must be a directory: $source")
}
