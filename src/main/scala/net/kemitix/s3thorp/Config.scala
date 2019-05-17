package net.kemitix.s3thorp

import java.io.File
import java.nio.file.Path

case class Config(bucket: Bucket = Bucket(""),
                  prefix: RemoteKey = RemoteKey(""),
                  verbose: Int = 1,
                  source: File
               ) {

  def relativePath(file: File): Path = source.toPath.relativize(file.toPath)

}
