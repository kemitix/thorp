package net.kemitix.s3thorp

import net.kemitix.s3thorp.aws.api.S3Action.{CopyS3Action, DeleteS3Action, UploadS3Action}
import net.kemitix.s3thorp.aws.api.S3Action
import net.kemitix.s3thorp.domain.Config

// Logging for the Sync class
object SyncLogging {

  def logRunStart(info: Int => String => Unit)(implicit c: Config): Unit =
    info(1)(s"Bucket: ${c.bucket.name}, Prefix: ${c.prefix.key}, Source: ${c.source}, " +
      s"Filter: ${c.filters.map{ f => f.filter}.mkString(""", """)} " +
      s"Exclude: ${c.excludes.map{ f => f.exclude}.mkString(""", """)}")

  def logFileScan(info: Int => String => Unit)(implicit c: Config): Unit =
    info(1)(s"Scanning local files: ${c.source}...")

  def logRunFinished(actions: List[S3Action],
                     info: Int => String => Unit)
                    (implicit c: Config): Unit = {
    val counters = actions.foldLeft(Counters())(logActivity)
    info(1)(s"Uploaded ${counters.uploaded} files")
    info(1)(s"Copied   ${counters.copied} files")
    info(1)(s"Deleted  ${counters.deleted} files")
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
