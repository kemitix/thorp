package net.kemitix.thorp.lib

import net.kemitix.eip.zio.MessageChannel.UChannel
import net.kemitix.eip.zio.{Message, MessageChannel}
import net.kemitix.thorp.config.Config
import net.kemitix.thorp.domain.Action.{DoNothing, ToCopy, ToUpload}
import net.kemitix.thorp.domain.RemoteObjects.{
  remoteHasHash,
  remoteKeyExists,
  remoteMatchesLocalFile
}
import net.kemitix.thorp.domain._
import net.kemitix.thorp.filesystem.{FileSystem, Hasher}
import net.kemitix.thorp.lib.FileScanner.Hashes
import net.kemitix.thorp.storage.Storage
import net.kemitix.throp.uishell.UIEvent
import zio._
import zio.clock.Clock

trait LocalFileSystem {
  def scanCopyUpload(
      uiChannel: UChannel[Any, UIEvent],
      remoteObjects: RemoteObjects,
      archive: ThorpArchive
  ): RIO[
    Clock with Hasher with FileSystem with Config with FileScanner with Storage,
    Seq[StorageQueueEvent]]
}
object LocalFileSystem extends LocalFileSystem {

  override def scanCopyUpload(
      uiChannel: UChannel[Any, UIEvent],
      remoteObjects: RemoteObjects,
      archive: ThorpArchive
  ): RIO[
    Clock with Hasher with FileSystem with Config with FileScanner with Storage,
    Seq[StorageQueueEvent]] =
    for {
      fileSender    <- FileScanner.scanSources
      actionCounter <- Ref.make(0)
      bytesCounter  <- Ref.make(0L)
      uploads       <- Ref.make(Map.empty[MD5Hash, Promise[Throwable, RemoteKey]])
      fileReceiver <- fileReceiver(uiChannel,
                                   remoteObjects,
                                   archive,
                                   uploads,
                                   actionCounter,
                                   bytesCounter)
      _      <- MessageChannel.pointToPoint(fileSender)(fileReceiver).runDrain
      events <- UIO(List.empty)
    } yield events

  private def fileReceiver(
      uiChannel: UChannel[Any, UIEvent],
      remoteObjects: RemoteObjects,
      archive: ThorpArchive,
      uploads: Ref[Map[MD5Hash, Promise[Throwable, RemoteKey]]],
      actionCounterRef: Ref[Int],
      bytesCounterRef: Ref[Long]
  ): UIO[MessageChannel.UReceiver[Clock with Config with Storage,
                                  FileScanner.ScannedFile]] =
    UIO { message =>
      val localFile = message.body
      for {
        _                   <- uiFileFound(uiChannel)(localFile)
        action              <- chooseAction(remoteObjects, uploads, uiChannel)(localFile)
        actionCounter       <- actionCounterRef.update(_ + 1)
        bytesCounter        <- bytesCounterRef.update(_ + action.size)
        actionChosenMessage <- Message.create(UIEvent.ActionChosen(action))
        _                   <- MessageChannel.send(uiChannel)(actionChosenMessage)
        sequencedAction = SequencedAction(action, actionCounter)
        event <- archive.update(sequencedAction, bytesCounter)
        actionFinishedMessage <- Message.create(
          UIEvent.ActionFinished(event, actionCounter, bytesCounter))
        _ <- MessageChannel.send(uiChannel)(actionFinishedMessage)
      } yield ()
    }

  private def uiFileFound(uiChannel: UChannel[Any, UIEvent])(
      localFile: LocalFile) =
    for {
      fileFoundMessage <- Message.create(UIEvent.FileFound(localFile))
      _                <- MessageChannel.send(uiChannel)(fileFoundMessage)
    } yield ()

  private def chooseAction(
      remoteObjects: RemoteObjects,
      uploads: Ref[Map[MD5Hash, Promise[Throwable, RemoteKey]]],
      uiChannel: UChannel[Any, UIEvent],
  )(localFile: LocalFile): ZIO[Config with Clock, Nothing, Action] = {
    for {
      remoteExists  <- remoteKeyExists(remoteObjects, localFile.remoteKey)
      remoteMatches <- remoteMatchesLocalFile(remoteObjects, localFile)
      remoteForHash <- remoteHasHash(remoteObjects, localFile.hashes)
      previous      <- uploads.get
      bucket        <- Config.bucket
      action <- if (remoteExists && remoteMatches)
        doNothing(localFile, bucket)
      else {
        remoteForHash match {
          case Some((sourceKey, hash)) =>
            doCopy(localFile, bucket, sourceKey, hash)
          case _ if (matchesPreviousUpload(previous, localFile.hashes)) =>
            doCopyWithPreviousUpload(localFile, bucket, previous, uiChannel)
          case _ =>
            doUpload(localFile, bucket)
        }
      }
    } yield action
  }

  private def matchesPreviousUpload(
      previous: Map[MD5Hash, Promise[Throwable, RemoteKey]],
      hashes: Hashes
  ): Boolean =
    hashes.exists({
      case (_, hash) => previous.contains(hash)
    })

  private def doNothing(
      localFile: LocalFile,
      bucket: Bucket
  ): UIO[Action] = UIO {
    DoNothing(bucket, localFile.remoteKey, localFile.length)
  }

  private def doCopy(
      localFile: LocalFile,
      bucket: Bucket,
      sourceKey: RemoteKey,
      hash: MD5Hash
  ): UIO[Action] = UIO {
    ToCopy(bucket, sourceKey, hash, localFile.remoteKey, localFile.length)
  }

  private def doCopyWithPreviousUpload(
      localFile: LocalFile,
      bucket: Bucket,
      previous: Map[MD5Hash, Promise[Throwable, RemoteKey]],
      uiChannel: UChannel[Any, UIEvent],
  ): ZIO[Clock, Nothing, Action] = {
    localFile.hashes
      .find({ case (_, hash) => previous.contains(hash) })
      .map({
        case (_, hash) =>
          for {
            awaitingMessage <- Message.create(
              UIEvent.AwaitingAnotherUpload(localFile.remoteKey, hash))
            _ <- MessageChannel.send(uiChannel)(awaitingMessage)
            action <- previous(hash).await.map(
              remoteKey =>
                ToCopy(bucket,
                       remoteKey,
                       hash,
                       localFile.remoteKey,
                       localFile.length))
            waitFinishedMessage <- Message.create(
              UIEvent.AnotherUploadWaitComplete(action))
            _ <- MessageChannel.send(uiChannel)(waitFinishedMessage)
          } yield action
      })
      .getOrElse(doUpload(localFile, bucket))
      .refineToOrDie[Nothing]
  }

  private def doUpload(
      localFile: LocalFile,
      bucket: Bucket
  ): UIO[Action] = {
    UIO(ToUpload(bucket, localFile, localFile.length))
  }

}
