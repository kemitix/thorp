package net.kemitix.thorp.core

import net.kemitix.thorp.domain.{Config, LocalFile, RemoteKey, S3ObjectsData}

sealed trait AppState

object AppState {

  case class Initial() extends AppState {
    def toConfigured(config: Config): Configured = Configured(config)
  }

  case class Configured(config: Config) extends AppState {
    def toScanLocalFiles(remoteData: S3ObjectsData,
                         localData: Stream[LocalFile]): ScanLocalFiles =
        ScanLocalFiles(config, remoteData, localData)
  }

  case class ScanLocalFiles(config: Config,
                            remoteData: S3ObjectsData,
                            localData: Stream[LocalFile]) extends AppState {
    def toScanRemoteKeys(actions: Stream[Action]): ScanRemoteKeys = {
      val streamRemoteKeys = remoteData.byKey.keys.toStream
      val setRemoteKeys = localData.map(_.remoteKey).toSet
      ScanRemoteKeys(config, streamRemoteKeys, setRemoteKeys, actions)
    }
  }

  case class ScanRemoteKeys(config: Config,
                            remoteData: Stream[RemoteKey],
                            localData: Set[RemoteKey],
                            actionsFromLocalFiles: Stream[Action]) extends AppState {
    def toCompleted(actions: Stream[Action]): Completed = {
      Completed(actionsFromLocalFiles ++ actions)
    }
  }

  case class Completed(actions: Stream[Action]) extends AppState

}