package net.kemitix.thorp.console

import net.kemitix.thorp.domain.{Bucket, RemoteKey, Sources}

sealed trait ConsoleOut {
  def en: String
}
object ConsoleOut {
  case class ValidConfig(
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
}
