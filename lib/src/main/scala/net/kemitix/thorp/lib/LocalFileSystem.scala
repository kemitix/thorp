package net.kemitix.thorp.lib

import net.kemitix.thorp.config.Configuration
import net.kemitix.thorp.domain.MessageChannel.MessageConsumer
import net.kemitix.thorp.domain.{RemoteObjects, _}
import net.kemitix.thorp.uishell.UIEvent

import scala.concurrent.Promise
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

trait LocalFileSystem {

  def scanCopyUpload(configuration: Configuration,
                     uiChannel: MessageConsumer[UIEvent],
                     remoteObjects: RemoteObjects,
                     archive: ThorpArchive): Seq[StorageEvent]

  def scanDelete(configuration: Configuration,
                 uiChannel: MessageConsumer[UIEvent],
                 remoteData: RemoteObjects,
                 archive: ThorpArchive): Seq[StorageEvent]

}
object LocalFileSystem extends LocalFileSystem {

  override def scanCopyUpload(configuration: Configuration,
                              uiChannel: MessageConsumer[UIEvent],
                              remoteObjects: RemoteObjects,
                              archive: ThorpArchive): Seq[StorageEvent] = {
    val fileSender = FileScanner.scanSources(configuration)
    val fileChannel = MessageChannel.create(fileSender)
    val actionCounter = 0
    val bytesCounter = 0L
    val uploads = Map.empty[MD5Hash, Promise[RemoteKey]]
    val events = List.empty[StorageEvent]
    fileChannel.addMessageConsumer(
      fileReceiver(
        configuration,
        uiChannel,
        remoteObjects,
        archive,
        uploads,
        actionCounter,
        bytesCounter,
        events
      )
    )
    fileChannel.startChannel()
    fileChannel.waitForShutdown()
    events
  }

  override def scanDelete(configuration: Configuration,
                          uiChannel: MessageConsumer[UIEvent],
                          remoteData: RemoteObjects,
                          archive: ThorpArchive): Seq[StorageEvent] = ???
//  {
//    val kSender = keySender(remoteData.byKey.keys.asScala)
//    for {
//      actionCounter <- Ref.make(0)
//      bytesCounter <- Ref.make(0L)
//      eventsRef <- Ref.make(List.empty[StorageEvent])
//      //      keyReceiver <- keyReceiver(
//      //        configuration,
//      //        uiChannel,
//      //        archive,
//      //        actionCounter,
//      //        bytesCounter,
//      //        eventsRef
//      //      )
//      parallel = configuration.parallel
//      //      _ <- MessageChannel
//      //        .pointToPointPar(parallel)(kSender)(keyReceiver)
//      //        .runDrain
//      events <- eventsRef.get
//    } yield events
//  }

  private def fileReceiver(
    configuration: Configuration,
    uiChannel: MessageConsumer[UIEvent],
    remoteObjects: RemoteObjects,
    archive: ThorpArchive,
    uploads: Map[MD5Hash, Promise[RemoteKey]],
    actionCounterRef: Int,
    bytesCounterRef: Long,
    events: List[StorageEvent]
  ): MessageConsumer[LocalFile] =
    ??? //TODO
//  new MessageConsumer()
//  UIO[MessageChannel.UReceiver[Clock, FileScanner.ScannedFile]] =
//    UIO { message =>
//      val localFile = message.body
//      for {
//        _ <- uiFileFound(uiChannel)(localFile)
//        action <- chooseAction(
//          configuration,
//          remoteObjects,
//          uploads,
//          uiChannel
//        )(localFile)
//        actionCounter <- actionCounterRef.update(_ + 1)
//        bytesCounter <- bytesCounterRef.update(_ + action.size)
//        _ <- uiActionChosen(uiChannel)(action)
//        sequencedAction = SequencedAction(action, actionCounter)
//        event <- archive.update(
//          configuration,
//          uiChannel,
//          sequencedAction,
//          bytesCounter
//        )
//        _ <- eventsRef.update(list => event :: list)
//        _ <- uiActionFinished(uiChannel)(
//          action,
//          actionCounter,
//          bytesCounter,
//          event
//        )
//      } yield ()
  //}

  private def uiActionChosen(
    uiChannel: MessageChannel.MessageConsumer[UIEvent]
  )(action: Action): Unit =
    uiChannel.accept(UIEvent.actionChosen(action))

  private def uiActionFinished(uiChannel: MessageConsumer[UIEvent])(
    action: Action,
    actionCounter: Int,
    bytesCounter: Long,
    event: StorageEvent
  ): Unit =
    uiChannel.accept(
      UIEvent.actionFinished(action, actionCounter, bytesCounter, event)
    )

  private def uiFileFound(
    uiChannel: MessageConsumer[UIEvent]
  )(localFile: LocalFile): Unit =
    uiChannel.accept(UIEvent.fileFound(localFile))

  private def chooseAction(configuration: Configuration,
                           remoteObjects: RemoteObjects,
                           uploads: Map[MD5Hash, RemoteKey],
                           uiChannel: MessageConsumer[UIEvent],
  )(localFile: LocalFile): Action = {
    val remoteExists = remoteObjects.remoteKeyExists(localFile.remoteKey)
    val remoteMatches = remoteObjects.remoteMatchesLocalFile(localFile)
    val remoteForHash = remoteObjects.remoteHasHash(localFile.hashes).toScala
    val previous = uploads
    val bucket = configuration.bucket
    val action = if (remoteExists && remoteMatches) {
      doNothing(localFile, bucket)
    } else {
      remoteForHash match {
        case pair: Some[Tuple[RemoteKey, MD5Hash]] =>
          val sourceKey = pair.value.a
          val hash = pair.value.b
          doCopy(localFile, bucket, sourceKey, hash)
        case _ if matchesPreviousUpload(previous, localFile.hashes) =>
          doCopyWithPreviousUpload(localFile, bucket, previous, uiChannel)
        case _ =>
          doUpload(localFile, bucket)
      }
    }
    action
  }

  private def matchesPreviousUpload(previous: Map[MD5Hash, RemoteKey],
                                    hashes: Hashes) =
    hashes
      .values()
      .stream()
      .anyMatch({ hash =>
        previous.contains(hash)
      })

  private def doNothing(localFile: LocalFile, bucket: Bucket) =
    Action.doNothing(bucket, localFile.remoteKey, localFile.length)

  private def doCopy(localFile: LocalFile,
                     bucket: Bucket,
                     sourceKey: RemoteKey,
                     hash: MD5Hash) =
    Action
      .toCopy(bucket, sourceKey, hash, localFile.remoteKey, localFile.length)

  private def doCopyWithPreviousUpload(localFile: LocalFile,
                                       bucket: Bucket,
                                       previous: Map[MD5Hash, RemoteKey],
                                       uiChannel: MessageConsumer[UIEvent],
  ) = {
    localFile.hashes
      .values()
      .stream()
      .filter({ hash =>
        previous.contains(hash)
      })
      .findFirst()
      .toScala
      .map({ hash =>
        {
          uiChannel
            .accept(UIEvent.awaitingAnotherUpload(localFile.remoteKey, hash))
          val action = Action.toCopy(
            bucket,
            previous(hash),
            hash,
            localFile.remoteKey,
            localFile.length
          )
          uiChannel.accept(UIEvent.anotherUploadWaitComplete(action))
          action
        }
      })
      .getOrElse(doUpload(localFile, bucket))
  }

  private def doUpload(localFile: LocalFile, bucket: Bucket) =
    Action.toUpload(bucket, localFile, localFile.length)

  def keySender(
    keys: Iterable[RemoteKey]
  ): MessageChannel.MessageSupplier[RemoteKey] =
    MessageChannel.createMessageSupplier(keys.asJavaCollection)

  def keyReceiver(
    configuration: Configuration,
    uiChannel: MessageConsumer[UIEvent],
    archive: ThorpArchive,
    actionCounter: Int,
    bytesCounter: Long,
    events: List[StorageEvent]
  ): MessageChannel.MessageConsumer[RemoteKey] =
    ??? //TODO
//    UIO { message =>
//      {
//        val remoteKey = message.body
//        for {
//          _ <- uiKeyFound(uiChannel)(remoteKey)
//          sources = configuration.sources
//          prefix = configuration.prefix
//          exists = FileSystem.hasLocalFile(sources, prefix, remoteKey)
//          _ <- ZIO.when(!exists) {
//            for {
//              actionCounter <- actionCounterRef.update(_ + 1)
//              bucket = configuration.bucket
//              action = Action.toDelete(bucket, remoteKey, 0L)
//              _ <- uiActionChosen(uiChannel)(action)
//              bytesCounter <- bytesCounterRef.update(_ + action.size)
//              sequencedAction = SequencedAction(action, actionCounter)
//              event <- archive.update(
//                configuration,
//                uiChannel,
//                sequencedAction,
//                0L
//              )
//              _ <- eventsRef.update(list => event :: list)
//              _ <- uiActionFinished(uiChannel)(
//                action,
//                actionCounter,
//                bytesCounter,
//                event
//              )
//            } yield ()
//          }
//        } yield ()
//      }
//    }

  private def uiKeyFound(
    uiChannel: MessageConsumer[UIEvent]
  )(remoteKey: RemoteKey): Unit =
    uiChannel.accept(UIEvent.keyFound(remoteKey))

}
