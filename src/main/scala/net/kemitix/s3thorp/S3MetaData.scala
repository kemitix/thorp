package net.kemitix.s3thorp

import java.io.File

case class S3MetaData(localFile: File,
                      remote: Option[(RemoteKey, MD5Hash, LastModified)])