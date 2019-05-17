package net.kemitix.s3thorp

import java.io.File

case class S3MetaData(localFile: File,
                      remotePath: RemoteKey,
                      remoteHash: MD5Hash,
                      remoteLastModified: LastModified)