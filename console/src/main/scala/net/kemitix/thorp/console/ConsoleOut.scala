package net.kemitix.thorp.console

import net.kemitix.thorp.domain.StorageEvent.ActionSummary
import net.kemitix.thorp.domain.Terminal._
import net.kemitix.thorp.domain.{Bucket, RemoteKey, Sources}
import zio.UIO

import scala.io.AnsiColor._

sealed trait ConsoleOut {
  def en: String
}

object ConsoleOut {

  sealed trait WithBatchMode {
    def en: String
    def enBatch: String
    def apply(batchMode: Boolean): UIO[String] =
      selectLine(batchMode)
    private def selectLine(batchMode: Boolean) =
      if (batchMode) UIO(enBatch) else UIO(en)
  }

  final case class ValidConfig(
      bucket: Bucket,
      prefix: RemoteKey,
      sources: Sources
  ) extends ConsoleOut {
    private val sourcesList = sources.paths.mkString(", ")
    override def en: String =
      List(s"Bucket: ${bucket.name}",
           s"Prefix: ${prefix.key}",
           s"Source: $sourcesList")
        .mkString(", ")
  }

  final case class UploadComplete(remoteKey: RemoteKey)
      extends ConsoleOut.WithBatchMode {
    override def en: String =
      s"${GREEN}Uploaded:$RESET ${remoteKey.key}$eraseToEndOfScreen"
    override def enBatch: String =
      s"Uploaded: ${remoteKey.key}"
  }

  final case class CopyComplete(sourceKey: RemoteKey, targetKey: RemoteKey)
      extends ConsoleOut.WithBatchMode {
    override def en: String =
      s"${GREEN}Copied:$RESET ${sourceKey.key} => ${targetKey.key}$eraseToEndOfScreen"
    override def enBatch: String =
      s"Copied: ${sourceKey.key} => ${targetKey.key}"
  }

  final case class DeleteComplete(remoteKey: RemoteKey)
      extends ConsoleOut.WithBatchMode {
    override def en: String =
      s"${GREEN}Deleted:$RESET ${remoteKey.key}$eraseToEndOfScreen"
    override def enBatch: String =
      s"Deleted: $remoteKey"
  }

  final case class ErrorQueueEventOccurred(action: ActionSummary, e: Throwable)
      extends ConsoleOut.WithBatchMode {
    override def en: String =
      s"${RED}ERROR:$RESET ${action.name} ${action.keys}: ${e.getMessage}$eraseToEndOfScreen"
    override def enBatch: String =
      s"${action.name} failed: ${action.keys}: ${e.getMessage}"
  }
}
