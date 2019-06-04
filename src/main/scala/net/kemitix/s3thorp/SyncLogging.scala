package net.kemitix.s3thorp

import net.kemitix.s3thorp.domain.Config

// Logging for the Sync class
object SyncLogging {

  def logRunStart(info: String => Unit)(implicit c: Config): Unit =
    info(s"Bucket: ${c.bucket.name}, Prefix: ${c.prefix.key}, Source: ${c.source}, " +
      s"Filter: ${c.filters.map{ f => f.filter}.mkString(""", """)} " +
      s"Exclude: ${c.excludes.map{ f => f.exclude}.mkString(""", """)}")

  def logFileScan(info: String => Unit)(implicit c: Config): Unit =
    info(s"Scanning local files: ${c.source}...")

  def logRunFinished(actions: List[S3Action],
                     info: String => Unit)
                    (implicit c: Config): Unit = {
    val counters = actions.foldLeft(Counters())(logActivity)
    info(s"Uploaded ${counters.uploaded} files")
    info(s"Copied   ${counters.copied} files")
    info(s"Deleted  ${counters.deleted} files")
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

}
