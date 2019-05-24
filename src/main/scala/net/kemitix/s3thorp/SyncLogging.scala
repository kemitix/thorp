package net.kemitix.s3thorp

import cats.effect.IO

// Logging for the Sync class
trait SyncLogging extends Logging {

  def logRunStart(implicit c: Config): Unit =
    log1(s"Bucket: ${c.bucket.name}, Prefix: ${c.prefix.key}, Source: ${c.source}, " +
      s"Filter: ${c.filters.map{f => f.filter}.mkString(""", """)}")(c)

  def logFileScan(implicit c: Config): Unit =
    log1(s"Scanning local files: ${c.source}...")


  def logRunFinished(actions: List[S3Action])
                    (implicit c: Config): Unit = {
    val counters = actions.foldLeft(Counters())(logActivity)
    log1(s"Uploaded ${counters.uploaded} files")
    log1(s"Copied   ${counters.copied} files")
    log1(s"Deleted  ${counters.deleted} files")
  }

  private def logActivity(implicit c: Config): (Counters, S3Action) => Counters =
    (counters: Counters, s3Action: S3Action) => {
      s3Action match {
        case UploadS3Action(remoteKey, _) =>
          counters.copy(uploaded = counters.uploaded + 1)
        case CopyS3Action(remoteKey) =>
          counters.copy(copied = counters.copied + 1)
        case DeleteS3Action(remoteKey) =>
          counters.copy(deleted = counters.deleted + 1)
        case _ => counters
      }
    }

  case class Counters(uploaded: Int = 0,
                      deleted: Int = 0,
                      copied: Int = 0)

}
