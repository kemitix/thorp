package net.kemitix.thorp.lib

import java.util.concurrent.atomic.AtomicReference

import net.kemitix.eip.zio.MessageChannel
import net.kemitix.eip.zio.MessageChannel.UChannel
import net.kemitix.thorp.config.ConfigOption.{
  IgnoreGlobalOptions,
  IgnoreUserOptions
}
import net.kemitix.thorp.config.{
  Config,
  ConfigOption,
  ConfigOptions,
  ConfigurationBuilder
}
import net.kemitix.thorp.domain.Action.{DoNothing, ToCopy, ToDelete, ToUpload}
import net.kemitix.thorp.domain._
import net.kemitix.thorp.filesystem.{FileSystem, Hasher, Resource}
import net.kemitix.thorp.storage.Storage
import net.kemitix.thorp.uishell.UIEvent
import net.kemitix.thorp.uishell.UIEvent.{
  ActionChosen,
  ActionFinished,
  FileFound,
  KeyFound
}
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import zio.clock.Clock
import zio.{DefaultRuntime, UIO, ZIO}

import scala.collection.MapView
import scala.jdk.CollectionConverters._

class LocalFileSystemTest extends FreeSpec {

  private val source       = Resource(this, "upload")
  private val sourcePath   = source.toPath
  private val sourceOption = ConfigOption.Source(sourcePath)
  private val bucket       = Bucket.named("bucket")
  private val bucketOption = ConfigOption.Bucket(bucket.name)
  private val configOptions = ConfigOptions(
    List[ConfigOption](
      sourceOption,
      bucketOption,
      IgnoreGlobalOptions,
      IgnoreUserOptions
    ))

  private val uiEvents = new AtomicReference[List[UIEvent]](List.empty)
  private val actions  = new AtomicReference[List[SequencedAction]](List.empty)

  private def archive: ThorpArchive = new ThorpArchive {
    override def update(uiChannel: UChannel[Any, UIEvent],
                        sequencedAction: SequencedAction,
                        totalBytesSoFar: Long)
      : ZIO[Storage with Config, Nothing, StorageEvent] = UIO {
      actions.updateAndGet(l => sequencedAction :: l)
      StorageEvent.DoNothingEvent(sequencedAction.action.remoteKey)
    }
  }

  private val runtime = new DefaultRuntime {}

  private object TestEnv
      extends Clock.Live
      with Hasher.Live
      with FileSystem.Live
      with Config.Live
      with FileScanner.Live
      with Storage.Test

  "scanCopyUpload" - {
    def sender(objects: RemoteObjects): UIO[MessageChannel.ESender[
      Clock with Hasher with FileSystem with Config with FileScanner with Config with Storage,
      Throwable,
      UIEvent]] =
      UIO { uiChannel =>
        (for {
          _ <- LocalFileSystem.scanCopyUpload(uiChannel, objects, archive)
        } yield ()) <* MessageChannel.endChannel(uiChannel)
      }
    def receiver(): UIO[MessageChannel.UReceiver[Any, UIEvent]] =
      UIO { message =>
        val uiEvent = message.body
        uiEvents.updateAndGet(l => uiEvent :: l)
        UIO(())
      }
    def program(remoteObjects: RemoteObjects) =
      for {
        config   <- ConfigurationBuilder.buildConfig(configOptions)
        _        <- Config.set(config)
        sender   <- sender(remoteObjects)
        receiver <- receiver()
        _        <- MessageChannel.pointToPoint(sender)(receiver).runDrain
      } yield ()
    "where remote has no objects" - {
      val remoteObjects = RemoteObjects.empty
      "upload all files" - {
        "update archive with upload actions" in {
          actions.set(List.empty)
          runtime.unsafeRunSync(program(remoteObjects).provide(TestEnv))
          val actionList: Set[Action] = actions.get.map(_.action).toSet
          actionList.filter(_.isInstanceOf[ToUpload]) should have size 2
          actionList.map(_.remoteKey) shouldEqual Set(
            MD5HashData.Root.remoteKey,
            MD5HashData.Leaf.remoteKey)
        }
        "ui is updated" in {
          uiEvents.set(List.empty)
          runtime.unsafeRunSync(program(remoteObjects).provide(TestEnv))
          val summary = uiEventsSummary
          summary should have size 6
          summary should contain inOrderElementsOf List(
            "file found : root-file",
            "action chosen : root-file : ToUpload",
            "action finished : root-file : ToUpload")
          summary should contain inOrderElementsOf List(
            "file found : subdir/leaf-file",
            "action chosen : subdir/leaf-file : ToUpload",
            "action finished : subdir/leaf-file : ToUpload"
          )
        }
      }
    }
    "where remote has all object" - {
      val remoteObjects =
        RemoteObjects.create(
          MapView(
            MD5HashData.Root.hash -> MD5HashData.Root.remoteKey,
            MD5HashData.Leaf.hash -> MD5HashData.Leaf.remoteKey).toMap.asJava,
          MapView(
            MD5HashData.Root.remoteKey -> MD5HashData.Root.hash,
            MD5HashData.Leaf.remoteKey -> MD5HashData.Leaf.hash).toMap.asJava
        )
      "do nothing for all files" - {
        "all archive actions do nothing" in {
          actions.set(List.empty)
          runtime.unsafeRunSync(program(remoteObjects).provide(TestEnv))
          val actionList: Set[Action] = actions.get.map(_.action).toSet
          actionList should have size 2
          actionList.filter(_.isInstanceOf[DoNothing]) should have size 2
        }
        "ui is updated" in {
          uiEvents.set(List.empty)
          runtime.unsafeRunSync(program(remoteObjects).provide(TestEnv))
          val summary = uiEventsSummary
          summary should have size 6
          summary should contain inOrderElementsOf List(
            "file found : root-file",
            "action chosen : root-file : DoNothing",
            "action finished : root-file : DoNothing")
          summary should contain inOrderElementsOf List(
            "file found : subdir/leaf-file",
            "action chosen : subdir/leaf-file : DoNothing",
            "action finished : subdir/leaf-file : DoNothing"
          )
        }
      }
    }
    "where remote has some objects" - {
      val remoteObjects =
        RemoteObjects.create(
          MapView(MD5HashData.Root.hash      -> MD5HashData.Root.remoteKey).toMap.asJava,
          MapView(MD5HashData.Root.remoteKey -> MD5HashData.Root.hash).toMap.asJava
        )
      "upload leaf, do nothing for root" - {
        "archive actions upload leaf" in {
          actions.set(List.empty)
          runtime.unsafeRunSync(program(remoteObjects).provide(TestEnv))
          val actionList: Set[Action] = actions.get.map(_.action).toSet
          actionList
            .filter(_.isInstanceOf[DoNothing])
            .map(_.remoteKey) shouldEqual Set(MD5HashData.Root.remoteKey)
          actionList
            .filter(_.isInstanceOf[ToUpload])
            .map(_.remoteKey) shouldEqual Set(MD5HashData.Leaf.remoteKey)
        }
        "ui is updated" in {
          uiEvents.set(List.empty)
          runtime.unsafeRunSync(program(remoteObjects).provide(TestEnv))
          val summary = uiEventsSummary
          summary should contain inOrderElementsOf List(
            "file found : root-file",
            "action chosen : root-file : DoNothing",
            "action finished : root-file : DoNothing")
          summary should contain inOrderElementsOf List(
            "file found : subdir/leaf-file",
            "action chosen : subdir/leaf-file : ToUpload",
            "action finished : subdir/leaf-file : ToUpload"
          )
        }
      }
    }
    "where remote objects are swapped" ignore {
      val remoteObjects =
        RemoteObjects.create(
          MapView(
            MD5HashData.Root.hash -> MD5HashData.Leaf.remoteKey,
            MD5HashData.Leaf.hash -> MD5HashData.Root.remoteKey).toMap.asJava,
          MapView(
            MD5HashData.Root.remoteKey -> MD5HashData.Leaf.hash,
            MD5HashData.Leaf.remoteKey -> MD5HashData.Root.hash).toMap.asJava
        )
      "copy files" - {
        "archive swaps objects" ignore {
          // TODO this is not supported
        }
      }
    }
    "where file has been renamed" - {
      // renamed from "other/root" to "root-file"
      val otherRootKey = RemoteKey.create("other/root")
      val remoteObjects =
        RemoteObjects.create(
          MapView(
            MD5HashData.Root.hash -> otherRootKey,
            MD5HashData.Leaf.hash -> MD5HashData.Leaf.remoteKey).toMap.asJava,
          MapView(
            otherRootKey               -> MD5HashData.Root.hash,
            MD5HashData.Leaf.remoteKey -> MD5HashData.Leaf.hash).toMap.asJava
        )
      "copy object and delete original" in {
        actions.set(List.empty)
        runtime.unsafeRunSync(program(remoteObjects).provide(TestEnv))
        val actionList: Set[Action] = actions.get.map(_.action).toSet
        actionList should have size 2
        actionList
          .filter(_.isInstanceOf[DoNothing])
          .map(_.remoteKey) shouldEqual Set(MD5HashData.Leaf.remoteKey)
        actionList
          .filter(_.isInstanceOf[ToCopy])
          .map(_.remoteKey) shouldEqual Set(MD5HashData.Root.remoteKey)
      }
      "ui is updated" in {
        uiEvents.set(List.empty)
        runtime.unsafeRunSync(program(remoteObjects).provide(TestEnv))
        val summary = uiEventsSummary
        summary should contain inOrderElementsOf List(
          "file found : root-file",
          "action chosen : root-file : ToCopy",
          "action finished : root-file : ToCopy")
        summary should contain inOrderElementsOf List(
          "file found : subdir/leaf-file",
          "action chosen : subdir/leaf-file : DoNothing",
          "action finished : subdir/leaf-file : DoNothing"
        )
      }
    }
  }

  "scanDelete" - {
    def sender(objects: RemoteObjects): UIO[
      MessageChannel.ESender[Clock with Config with FileSystem with Storage,
                             Throwable,
                             UIEvent]] =
      UIO { uiChannel =>
        (for {
          _ <- LocalFileSystem.scanDelete(uiChannel, objects, archive)
        } yield ()) <* MessageChannel.endChannel(uiChannel)
      }
    def receiver(): UIO[MessageChannel.UReceiver[Any, UIEvent]] =
      UIO { message =>
        val uiEvent = message.body
        uiEvents.updateAndGet(l => uiEvent :: l)
        UIO(())
      }
    def program(remoteObjects: RemoteObjects) = {
      for {
        config   <- ConfigurationBuilder.buildConfig(configOptions)
        _        <- Config.set(config)
        sender   <- sender(remoteObjects)
        receiver <- receiver()
        _        <- MessageChannel.pointToPoint(sender)(receiver).runDrain
      } yield ()
    }
    "where remote has no extra objects" - {
      val remoteObjects = RemoteObjects.create(
        MapView(
          MD5HashData.Root.hash -> MD5HashData.Root.remoteKey,
          MD5HashData.Leaf.hash -> MD5HashData.Leaf.remoteKey).toMap.asJava,
        MapView(
          MD5HashData.Root.remoteKey -> MD5HashData.Root.hash,
          MD5HashData.Leaf.remoteKey -> MD5HashData.Leaf.hash).toMap.asJava
      )
      "do nothing for all files" - {
        "no archive actions" in {
          actions.set(List.empty)
          runtime.unsafeRunSync(program(remoteObjects).provide(TestEnv))
          val actionList: Set[Action] = actions.get.map(_.action).toSet
          actionList should have size 0
        }
        "ui is updated" in {
          uiEvents.set(List.empty)
          runtime.unsafeRunSync(program(remoteObjects).provide(TestEnv))
          uiEventsSummary shouldEqual List("key found: root-file",
                                           "key found: subdir/leaf-file")
        }
      }
    }
    "where remote has extra objects" - {
      val extraHash   = MD5Hash.create("extra")
      val extraObject = RemoteKey.create("extra")
      val remoteObjects = RemoteObjects.create(
        MapView(MD5HashData.Root.hash      -> MD5HashData.Root.remoteKey,
                MD5HashData.Leaf.hash      -> MD5HashData.Leaf.remoteKey,
                extraHash                  -> extraObject).toMap.asJava,
        MapView(MD5HashData.Root.remoteKey -> MD5HashData.Root.hash,
                MD5HashData.Leaf.remoteKey -> MD5HashData.Leaf.hash,
                extraObject                -> extraHash).toMap.asJava
      )
      "remove the extra object" - {
        "archive delete action" in {
          actions.set(List.empty)
          runtime.unsafeRunSync(program(remoteObjects).provide(TestEnv))
          val actionList: Set[Action] = actions.get.map(_.action).toSet
          actionList should have size 1
          actionList
            .filter(_.isInstanceOf[ToDelete])
            .map(_.remoteKey) shouldEqual Set(extraObject)
        }
        "ui is updated" in {
          uiEvents.set(List.empty)
          runtime.unsafeRunSync(program(remoteObjects).provide(TestEnv))
          uiEventsSummary shouldEqual List(
            "key found: root-file",
            "key found: subdir/leaf-file",
            "key found: extra",
            "action chosen : extra : ToDelete",
            "action finished : extra : ToDelete"
          )
        }
      }
    }
  }

  private def uiEventsSummary: List[String] = {
    uiEvents
      .get()
      .reverse
      .map {
        case FileFound(localFile) =>
          String.format("file found : %s", localFile.remoteKey.key)
        case ActionChosen(action) =>
          String.format("action chosen : %s : %s",
                        action.remoteKey.key,
                        action.getClass.getSimpleName)
        case ActionFinished(action, actionCounter, bytesCounter, event) =>
          String.format("action finished : %s : %s",
                        action.remoteKey.key,
                        action.getClass.getSimpleName)
        case KeyFound(remoteKey) =>
          String.format("key found: %s", remoteKey.key)
        case x => String.format("unknown : %s", x.getClass.getSimpleName)
      }
  }

}
