package net.kemitix.s3thorp

case class S3MetaData(localFile: LocalFile,
                      remote: Option[RemoteMetaData])