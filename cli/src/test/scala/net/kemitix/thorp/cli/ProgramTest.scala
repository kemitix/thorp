package net.kemitix.thorp.cli

import java.io.File

import cats.data.EitherT
import cats.effect.IO
import net.kemitix.thorp.core.Action.{ToCopy, ToDelete, ToUpload}
import net.kemitix.thorp.core._
import net.kemitix.thorp.domain.{Bucket, LocalFile, Logger, MD5Hash, RemoteKey, StorageQueueEvent}
import net.kemitix.thorp.storage.api.{HashService, StorageService}
import org.scalatest.FunSpec

class ProgramTest extends FunSpec {

  val source: File = Resource(this, ".")
  val bucket: Bucket = Bucket("aBucket")
  val hash: MD5Hash = MD5Hash("aHash")
  val copyAction: Action = ToCopy(bucket, RemoteKey("copy-me"), hash, RemoteKey("overwrite-me"), 17L)
  val uploadAction: Action = ToUpload(bucket, LocalFile.resolve("aFile", Map(), source, _ => RemoteKey("upload-me")), 23L)
  val deleteAction: Action = ToDelete(bucket, RemoteKey("delete-me"), 0L)

  val configOptions: ConfigOptions = ConfigOptions(options = List(
    ConfigOption.IgnoreGlobalOptions,
    ConfigOption.IgnoreUserOptions
  ))

  describe("upload, copy and delete actions in plan") {
    val archive = TestProgram.thorpArchive
    it("should be handled in correct order") {
      val expected = List(copyAction, uploadAction, deleteAction)
      TestProgram.run(configOptions).unsafeRunSync
      val result = archive.actions
      assertResult(expected)(result)
    }
  }

  object TestProgram extends Program with TestPlanBuilder {
    val thorpArchive: ActionCaptureArchive = new ActionCaptureArchive
    override def thorpArchive(cliOptions: ConfigOptions, syncPlan: SyncPlan): IO[ThorpArchive] =
      IO.pure(thorpArchive)
  }

  trait TestPlanBuilder extends PlanBuilder {
    override def createPlan(storageService: StorageService,
                            hashService: HashService,
                            configOptions: ConfigOptions)
                           (implicit l: Logger): EitherT[IO, List[String], SyncPlan] = {
      EitherT.right(IO(SyncPlan(Stream(copyAction, uploadAction, deleteAction))))
    }
  }

  class ActionCaptureArchive extends ThorpArchive {
    var actions: List[Action] = List[Action]()
    override def update(index: Int,
                        action: Action,
                        totalBytesSoFar: Long)
                       (implicit l: Logger): Stream[IO[StorageQueueEvent]] = {
      actions = action :: actions
      Stream()
    }
  }

}

