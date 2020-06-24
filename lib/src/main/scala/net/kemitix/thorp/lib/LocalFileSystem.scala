package net.kemitix.thorp.lib

import java.util
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

import net.kemitix.thorp.config.Configuration
import net.kemitix.thorp.domain.Channel.{Listener, Sink}
import net.kemitix.thorp.domain.{RemoteObjects, _}
import net.kemitix.thorp.filesystem.FileSystem
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

    // state for the file receiver
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

    fileChannel.run(
      sink => FileScanner.scanSources(configuration, sink),
      "scan-sources"
    )

    fileChannel.start()
    fileChannel.waitForShutdown()
    events.asScala.toList
  }

  override def scanDelete(configuration: Configuration,
                          uiSink: Channel.Sink[UIEvent],
                          remoteData: RemoteObjects,
                          archive: ThorpArchive): Seq[StorageEvent] = {
    val deletionsChannel: Channel[RemoteKey] = Channel.create("deletions")

    // state for the file receiver
    val actionCounter = new AtomicInteger()
    val bytesCounter = new AtomicLong()
    val events = new util.LinkedList[StorageEvent]

    deletionsChannel.addListener(
      keyReceiver(
        configuration,
        uiSink,
        archive,
        actionCounter,
        bytesCounter,
        events
      )
    )

    deletionsChannel.run(sink => {
      remoteData.byKey.keys().forEach(key => sink.accept(key))
      sink.shutdown()
    }, "delete-source")

    deletionsChannel.start()
    deletionsChannel.waitForShutdown()
    events.asScala.toList
  }

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

  def keyReceiver(configuration: Configuration,
                  uiSink: Channel.Sink[UIEvent],
                  archive: ThorpArchive,
                  actionCounter: AtomicInteger,
                  bytesCounter: AtomicLong,
                  events: util.Deque[StorageEvent]): Listener[RemoteKey] = {
    (remoteKey: RemoteKey) =>
      {
        uiKeyFound(uiSink)(remoteKey)
        val sources = configuration.sources
        val prefix = configuration.prefix
        val exists = FileSystem.hasLocalFile(sources, prefix, remoteKey)
        if (!exists) {
          actionCounter.incrementAndGet()
          val bucket = configuration.bucket
          val action = Action.toDelete(bucket, remoteKey, 0L)
          uiActionChosen(uiSink)(action)
          bytesCounter.addAndGet(action.size)
          val sequencedAction = SequencedAction(action, actionCounter.get())
          val event = archive.update(configuration, uiSink, sequencedAction, 0L)
          events.addFirst(event)
          uiActionFinished(uiSink)(
            action,
            actionCounter.get(),
            bytesCounter.get(),
            event
          )
        }
      }
  }

  private def uiKeyFound(uiSink: Sink[UIEvent])(remoteKey: RemoteKey): Unit =
    uiSink.accept(UIEvent.keyFound(remoteKey))

}
