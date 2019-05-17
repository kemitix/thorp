package net.kemitix.s3thorp

import java.io.File
import java.nio.file.Path

import net.kemitix.s3thorp.Sync.LocalFile

case class Config(bucket: Bucket = Bucket(""),
                  prefix: String = "",
                  verbose: Int = 1,
                  source: LocalFile
               ) {

  def relativePath(file: File): Path = source.toPath.relativize(file.toPath)

}
