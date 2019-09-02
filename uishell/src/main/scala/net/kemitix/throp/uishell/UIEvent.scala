package net.kemitix.throp.uishell

import java.io.File

import net.kemitix.thorp.domain.{Counters, HashType, MD5Hash}

sealed trait UIEvent
object UIEvent {
  case object ShowValidConfig extends UIEvent

  case class RemoteDataFetched(size: Int) extends UIEvent

  case class ShowSummary(counters: Counters) extends UIEvent

  case class FileFound(file: File, hashes: Map[HashType, MD5Hash])
      extends UIEvent

}
