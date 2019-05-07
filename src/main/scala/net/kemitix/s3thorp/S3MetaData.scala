package net.kemitix.s3thorp

import net.kemitix.s3thorp.Sync.{Hash, LastModified, LocalFile, RemotePath}

case class S3MetaData(localFile: LocalFile,
                      remotePath: RemotePath,
                      remoteHash: Hash,
                      remoteLastModified: LastModified)