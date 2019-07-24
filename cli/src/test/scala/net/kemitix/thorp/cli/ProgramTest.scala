package net.kemitix.thorp.cli

import java.io.File
import java.nio.file.Path

import net.kemitix.thorp.console.Console
import net.kemitix.thorp.core.Action.{ToCopy, ToDelete, ToUpload}
import net.kemitix.thorp.core._
import net.kemitix.thorp.domain.StorageQueueEvent.DoNothingQueueEvent
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.{HashService, StorageService}
import org.scalatest.FunSpec
import zio.internal.PlatformLive
import zio.{Runtime, Task, TaskR}

class ProgramTest extends FunSpec {

  private val runtime = Runtime(Console.Live, PlatformLive.Default)

  val source: File     = Resource(this, ".")
  val sourcePath: Path = source.toPath
  val bucket: Bucket   = Bucket("aBucket")
  val hash: MD5Hash    = MD5Hash("aHash")
  val copyAction: Action =
    ToCopy(bucket, RemoteKey("copy-me"), hash, RemoteKey("overwrite-me"), 17L)
  val uploadAction: Action = ToUpload(
    bucket,
    LocalFile.resolve("aFile", Map(), sourcePath, _ => RemoteKey("upload-me")),
    23L)
  val deleteAction: Action = ToDelete(bucket, RemoteKey("delete-me"), 0L)

  val args: List[String] = List("--no-global", "--no-user")

  describe("upload, copy and delete actions in plan") {
    val archive = TestProgram.thorpArchive
    it("should be handled in correct order") {
      val expected = List(copyAction, uploadAction, deleteAction)
      invoke(args)
      val result = archive.actions.reverse
      assertResult(expected)(result)
    }
  }

  private def invoke(args: List[String]) =
    runtime.unsafeRunSync {
      TestProgram.run(args)
    }.toEither

  trait TestPlanBuilder extends PlanBuilder {
    override def createPlan(
        storageService: StorageService,
        hashService: HashService,
        configOptions: ConfigOptions
    ): Task[SyncPlan] = {
      Task(SyncPlan(Stream(copyAction, uploadAction, deleteAction)))
    }
  }

  class ActionCaptureArchive extends ThorpArchive {
    var actions: List[Action] = List[Action]()
    override def update(
        index: Int,
        action: Action,
        totalBytesSoFar: Long
    ): TaskR[Console, StorageQueueEvent] = {
      actions = action :: actions
      TaskR(DoNothingQueueEvent(RemoteKey("")))
    }
  }

  object TestProgram extends Program with TestPlanBuilder {
    val thorpArchive: ActionCaptureArchive = new ActionCaptureArchive
    override def thorpArchive(
        cliOptions: ConfigOptions,
        syncTotals: SyncTotals
    ): Task[ThorpArchive] =
      Task(thorpArchive)
  }

}
