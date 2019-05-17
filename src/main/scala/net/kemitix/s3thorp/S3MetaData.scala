package net.kemitix.s3thorp

import net.kemitix.s3thorp.Sync.LocalFile

case class S3MetaData(localFile: LocalFile,
                      remotePath: RemoteKey,
                      remoteHash: MD5Hash,
                      remoteLastModified: LastModified)