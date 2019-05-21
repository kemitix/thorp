package net.kemitix.s3thorp

import cats.implicits._
import cats.effect.IO
import net.kemitix.s3thorp.awssdk.S3Client

class Sync(s3Client: S3Client)
  extends LocalFileStream
    with S3MetaDataEnricher
    with ActionGenerator
    with ActionSubmitter
    with Logging {

  def run(implicit c: Config): IO[Unit] = {
    log1(s"Bucket: ${c.bucket.name}, Prefix: ${c.prefix.key}, Source: ${c.source}")
    listObjects(c.bucket, c.prefix)
      .map { implicit s3ObjectsData => {
        val actions = (for {
          file <- findFiles(c.source)
          meta = getMetadata(file)
          action <- createActions(meta)
          ioS3Action = submitAction(action)
        } yield ioS3Action).sequence
        val copyActions = actions.flatMap { sa =>
          IO {
            sa.filter {
              case CopyS3Action(_) => true
              case _ => false
            }
          }
        }
        val uploadActions = actions.flatMap { sa =>
          IO {
            sa.filter {
              case UploadS3Action(_, _) => true
              case _ => false
            }
          }
        }
        val deleteActions = actions.flatMap { sa =>
          IO {
            sa.filter {
              case DeleteS3Action(_) => true
              case _ => false
            }
          }
        }
        val sequencedActions =
          copyActions.flatMap{ sca => {
            uploadActions.flatMap{ sua => {
              deleteActions.flatMap{ sda => {
                IO { sca.combine(sua).combine(sda) }
              }}
            }}
          }}
        val counters = sequencedActions.unsafeRunSync
          .foldLeft(Counters())((counters: Counters, s3Action: S3Action) => {
            s3Action match {
              case UploadS3Action(remoteKey, _) =>
                log1(s"- Uploaded: ${remoteKey.key}")
                counters.copy(uploaded = counters.uploaded + 1)
              case CopyS3Action(remoteKey) =>
                log1(s"-   Copied: ${remoteKey.key}")
                counters.copy(copied = counters.copied + 1)
              case DeleteS3Action(remoteKey) =>
                log1(s"-  Deleted: ${remoteKey.key}")
                counters.copy(deleted = counters.deleted + 1)
              case _ => counters
            }
          })
        log1(s"Uploaded ${counters.uploaded} files")
        log1(s"Copied   ${counters.copied} files")
        log1(s"Moved    ${counters.moved} files")
        log1(s"Deleted  ${counters.deleted} files")
      }}
  }

  case class Counters(uploaded: Int = 0,
                      deleted: Int = 0,
                      copied: Int = 0,
                      moved: Int = 0)

  override def upload(localFile: LocalFile,
                      bucket: Bucket) =
    s3Client.upload(localFile, bucket)

  override def copy(bucket: Bucket,
                    sourceKey: RemoteKey,
                    hash: MD5Hash,
                    targetKey: RemoteKey) =
    s3Client.copy(bucket, sourceKey, hash, targetKey)

  override def delete(bucket: Bucket,
                      remoteKey: RemoteKey) =
    s3Client.delete(bucket, remoteKey)

  override def listObjects(bucket: Bucket,
                           prefix: RemoteKey
                          ) =
    s3Client.listObjects(bucket, prefix)
}
