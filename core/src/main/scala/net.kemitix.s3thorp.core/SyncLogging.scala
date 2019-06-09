package net.kemitix.s3thorp.core

import cats.effect.IO
import net.kemitix.s3thorp.aws.api.S3Action
import net.kemitix.s3thorp.aws.api.S3Action.{CopyS3Action, DeleteS3Action, UploadS3Action}
import net.kemitix.s3thorp.domain.Config

// Logging for the Sync class
object SyncLogging {

  def logRunStart[F[_]](info: Int => String => Unit)(implicit c: Config): IO[Unit] = IO {
    info(1)(s"Bucket: ${c.bucket.name}, Prefix: ${c.prefix.key}, Source: ${c.source}, ")}

  def logFileScan(info: Int => String => Unit)(implicit c: Config): IO[Unit] = IO{
    info(1)(s"Scanning local files: ${c.source}...")}

  def logRunFinished(actions: Stream[S3Action],
                     info: Int => String => Unit)
                    (implicit c: Config): IO[Unit] = IO {
    val counters = actions.foldLeft(Counters())(countActivities)
    info(1)(s"Uploaded ${counters.uploaded} files")
    info(1)(s"Copied   ${counters.copied} files")
    info(1)(s"Deleted  ${counters.deleted} files")
  }

  private def countActivities(implicit c: Config): (Counters, S3Action) => Counters =
    (counters: Counters, s3Action: S3Action) => {
      s3Action match {
        case _: UploadS3Action =>
          counters.copy(uploaded = counters.uploaded + 1)
        case _: CopyS3Action =>
          counters.copy(copied = counters.copied + 1)
        case _: DeleteS3Action =>
          counters.copy(deleted = counters.deleted + 1)
        case _ => counters
      }
    }

}
