package net.kemitix.s3thorp

import net.kemitix.s3thorp.Sync.{Hash, LastModified, LocalPath, RemotePath}

case class S3MetaData(localPath: LocalPath,
                      remotePath: RemotePath,
                      remoteHash: Hash,
                      remoteLastModified: LastModified)