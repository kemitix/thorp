package net.kemitix.thorp.lib

import java.util.concurrent.atomic.AtomicReference

import net.kemitix.eip.zio.MessageChannel
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
import net.kemitix.thorp.domain.Action.{DoNothing, ToCopy, ToUpload}
import net.kemitix.thorp.domain._
import net.kemitix.thorp.filesystem.{FileSystem, Hasher, Resource}
import net.kemitix.thorp.storage.Storage
import net.kemitix.throp.uishell.UIEvent
import net.kemitix.throp.uishell.UIEvent._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import zio.clock.Clock
import zio.{DefaultRuntime, UIO}

import scala.collection.MapView

class LocalFileSystemTest extends FreeSpec {

  private val source       = Resource(this, "upload")
  private val sourcePath   = source.toPath
  private val sourceOption = ConfigOption.Source(sourcePath)
  private val bucket       = Bucket("bucket")
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

  private def archive: ThorpArchive =
    (sequencedAction: SequencedAction, _) =>
      UIO {
        actions.updateAndGet(l => sequencedAction :: l)
        StorageEvent.DoNothingEvent(sequencedAction.action.remoteKey)
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
    def program(remoteObjects: RemoteObjects) = {
      for {
        config   <- ConfigurationBuilder.buildConfig(configOptions)
        _        <- Config.set(config)
        sender   <- sender(remoteObjects)
        receiver <- receiver()
        _        <- MessageChannel.pointToPoint(sender)(receiver).runDrain
      } yield ()
    }
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
          uiEventsSummary shouldEqual List(
            "file found : root-file",
            "action chosen : root-file : ToUpload",
            "action finished : root-file : ToUpload : 1 : 55",
            "file found : subdir/leaf-file",
            "action chosen : subdir/leaf-file : ToUpload",
            "action finished : subdir/leaf-file : ToUpload : 2 : 113"
          )
        }
      }
    }
    "where remote has all object" - {
      val remoteObjects =
        RemoteObjects(
          byHash = MapView(MD5HashData.Root.hash     -> MD5HashData.Root.remoteKey,
                           MD5HashData.Leaf.hash     -> MD5HashData.Leaf.remoteKey),
          byKey = MapView(MD5HashData.Root.remoteKey -> MD5HashData.Root.hash,
                          MD5HashData.Leaf.remoteKey -> MD5HashData.Leaf.hash)
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
          uiEventsSummary shouldEqual List(
            "file found : root-file",
            "action chosen : root-file : DoNothing",
            "action finished : root-file : DoNothing : 1 : 55",
            "file found : subdir/leaf-file",
            "action chosen : subdir/leaf-file : DoNothing",
            "action finished : subdir/leaf-file : DoNothing : 2 : 113"
          )
        }
      }
    }
    "where remote has some objects" - {
      val remoteObjects =
        RemoteObjects(
          byHash = MapView(MD5HashData.Root.hash     -> MD5HashData.Root.remoteKey),
          byKey = MapView(MD5HashData.Root.remoteKey -> MD5HashData.Root.hash)
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
          uiEventsSummary shouldEqual List(
            "file found : root-file",
            "action chosen : root-file : DoNothing",
            "action finished : root-file : DoNothing : 1 : 55",
            "file found : subdir/leaf-file",
            "action chosen : subdir/leaf-file : ToUpload",
            "action finished : subdir/leaf-file : ToUpload : 2 : 113"
          )
        }
      }
    }
    "where remote objects are swapped" ignore {
      val remoteObjects =
        RemoteObjects(
          byHash = MapView(MD5HashData.Root.hash     -> MD5HashData.Leaf.remoteKey,
                           MD5HashData.Leaf.hash     -> MD5HashData.Root.remoteKey),
          byKey = MapView(MD5HashData.Root.remoteKey -> MD5HashData.Leaf.hash,
                          MD5HashData.Leaf.remoteKey -> MD5HashData.Root.hash)
        )
      "copy files" - {
        "archive swaps objects" ignore {
          // TODO this is not supported
        }
      }
    }
    "where file has been renamed" - {
      // renamed from "other/root" to "root-file"
      val otherRootKey = RemoteKey("other/root")
      val remoteObjects =
        RemoteObjects(
          byHash = MapView(MD5HashData.Root.hash     -> otherRootKey,
                           MD5HashData.Leaf.hash     -> MD5HashData.Leaf.remoteKey),
          byKey = MapView(otherRootKey               -> MD5HashData.Root.hash,
                          MD5HashData.Leaf.remoteKey -> MD5HashData.Leaf.hash)
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
        uiEventsSummary shouldEqual List(
          "file found : root-file",
          "action chosen : root-file : ToCopy",
          "action finished : root-file : ToCopy : 1 : 55",
          "file found : subdir/leaf-file",
          "action chosen : subdir/leaf-file : DoNothing",
          "action finished : subdir/leaf-file : DoNothing : 2 : 113"
        )
      }
    }
  }

  "scanDelete" ignore {}

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
        case ActionFinished(action, actionCounter, bytesCounter) =>
          String.format("action finished : %s : %s : %s : %s",
                        action.remoteKey.key,
                        action.getClass.getSimpleName,
                        actionCounter,
                        bytesCounter)
        case _ => ""
      }
  }

}
