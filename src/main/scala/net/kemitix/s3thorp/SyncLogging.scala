package net.kemitix.s3thorp

import cats.effect.IO

// Logging for the Sync class
trait SyncLogging extends Logging {

  def log(completedActions: Stream[S3Action])
         (implicit c: Config): IO[Unit] = {
    val counters = completedActions
    .foldLeft(Counters())(logActivity)
    log1(s"Uploaded ${counters.uploaded} files")
    log1(s"Copied   ${counters.copied} files")
    log1(s"Moved    ${counters.moved} files")
    log1(s"Deleted  ${counters.deleted} files")
  }

  private def logActivity(implicit c: Config): (Counters, S3Action) => Counters =
    (counters: Counters, s3Action: S3Action) => {
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
    }

  case class Counters(uploaded: Int = 0,
                      deleted: Int = 0,
                      copied: Int = 0,
                      moved: Int = 0)

}
