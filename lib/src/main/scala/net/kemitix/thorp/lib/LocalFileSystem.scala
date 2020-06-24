package net.kemitix.thorp.lib

import java.util
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

import net.kemitix.thorp.config.Configuration
import net.kemitix.thorp.domain.Channel.Listener
import net.kemitix.thorp.domain.{RemoteObjects, _}
import net.kemitix.thorp.uishell.UIEvent

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

trait LocalFileSystem {

  def scanCopyUpload(configuration: Configuration,
                     uiSink: Channel.Sink[UIEvent],
                     remoteObjects: RemoteObjects,
                     archive: ThorpArchive): Seq[StorageEvent]

  def scanDelete(configuration: Configuration,
                 uiSink: Channel.Sink[UIEvent],
                 remoteData: RemoteObjects,
                 archive: ThorpArchive): Seq[StorageEvent]

}
object LocalFileSystem extends LocalFileSystem {

  override def scanCopyUpload(configuration: Configuration,
                              uiSink: Channel.Sink[UIEvent],
                              remoteObjects: RemoteObjects,
                              archive: ThorpArchive): Seq[StorageEvent] = {

    val fileChannel: Channel[LocalFile] = Channel.create("files")
    fileChannel.run(
      sink => FileScanner.scanSources(configuration, sink),
      "scan-sources"
    )

    val actionCounter = new AtomicInteger()
    val bytesCounter = new AtomicLong()
    val uploads = Map.empty[MD5Hash, RemoteKey]
    val events = new util.LinkedList[StorageEvent]
    fileChannel.addListener(
      fileReceiver(
        configuration,
        uiSink,
        remoteObjects,
        archive,
        uploads,
        actionCounter,
        bytesCounter,
        events
      )
    )
    fileChannel.start()
    fileChannel.waitForShutdown()
    events.asScala.toList
  }

  override def scanDelete(configuration: Configuration,
                          uiSink: Channel.Sink[UIEvent],
                          remoteData: RemoteObjects,
                          archive: ThorpArchive): Seq[StorageEvent] = List.empty
  ///???

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
    uiSink: Channel.Sink[UIEvent],
    remoteObjects: RemoteObjects,
    archive: ThorpArchive,
    uploads: Map[MD5Hash, RemoteKey],
    actionCounter: AtomicInteger,
    bytesCounter: AtomicLong,
    events: util.Deque[StorageEvent]
  ): Listener[LocalFile] = { (localFile: LocalFile) =>
    {
      uiFileFound(uiSink)(localFile)
      val action =
        chooseAction(configuration, remoteObjects, uploads, uiSink)(localFile)
      actionCounter.incrementAndGet()
      bytesCounter.addAndGet(action.size)
      uiActionChosen(uiSink)(action)
      val sequencedAction = SequencedAction(action, actionCounter.get())
      val event = archive
        .update(configuration, uiSink, sequencedAction, bytesCounter.get)
      events.addFirst(event)
      uiActionFinished(uiSink)(
        action,
        actionCounter.get,
        bytesCounter.get,
        event
      )
    }
  }

  private def uiActionChosen(
    uiSink: Channel.Sink[UIEvent]
  )(action: Action): Unit =
    uiSink.accept(UIEvent.actionChosen(action))

  private def uiActionFinished(uiSink: Channel.Sink[UIEvent])(
    action: Action,
    actionCounter: Int,
    bytesCounter: Long,
    event: StorageEvent
  ): Unit =
    uiSink.accept(
      UIEvent.actionFinished(action, actionCounter, bytesCounter, event)
    )

  private def uiFileFound(
    uiSink: Channel.Sink[UIEvent]
  )(localFile: LocalFile): Unit =
    uiSink.accept(UIEvent.fileFound(localFile))

  private def chooseAction(configuration: Configuration,
                           remoteObjects: RemoteObjects,
                           uploads: Map[MD5Hash, RemoteKey],
                           uiSink: Channel.Sink[UIEvent],
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
          doCopyWithPreviousUpload(localFile, bucket, previous, uiSink)
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
                                       uiSink: Channel.Sink[UIEvent],
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
          uiSink
            .accept(UIEvent.awaitingAnotherUpload(localFile.remoteKey, hash))
          val action = Action.toCopy(
            bucket,
            previous(hash),
            hash,
            localFile.remoteKey,
            localFile.length
          )
          uiSink.accept(UIEvent.anotherUploadWaitComplete(action))
          action
        }
      })
      .getOrElse(doUpload(localFile, bucket))
  }

  private def doUpload(localFile: LocalFile, bucket: Bucket) =
    Action.toUpload(bucket, localFile, localFile.length)

//  def keySender(
//    keys: Iterable[RemoteKey]
//  ): MessageChannel.MessageSupplier[RemoteKey] =
//    MessageChannel.createMessageSupplier(keys.asJavaCollection)

//  def keyReceiver(
//    configuration: Configuration,
//    uiChannel: MessageConsumer[UIEvent],
//    archive: ThorpArchive,
//    actionCounter: Int,
//    bytesCounter: Long,
//    events: List[StorageEvent]
//  ): MessageChannel.MessageConsumer[RemoteKey] =
//    ??? //TODO
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

//  private def uiKeyFound(
//    uiChannel: MessageConsumer[UIEvent]
//  )(remoteKey: RemoteKey): Unit =
//    uiChannel.accept(UIEvent.keyFound(remoteKey))

}
