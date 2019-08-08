package net.kemitix.thorp.core

import java.io.File
import java.nio.file.Path

import net.kemitix.thorp.config.{
  Config,
  ConfigOption,
  ConfigOptions,
  ConfigurationBuilder
}
import net.kemitix.thorp.console._
import net.kemitix.thorp.core.Action.{DoNothing, ToCopy, ToDelete, ToUpload}
import net.kemitix.thorp.core.hasher.Hasher
import net.kemitix.thorp.domain.HashType.MD5
import net.kemitix.thorp.domain._
import net.kemitix.thorp.filesystem._
import net.kemitix.thorp.storage.api.Storage
import org.scalatest.FreeSpec
import zio.{DefaultRuntime, Task, UIO}

class PlanBuilderTest extends FreeSpec with TemporaryFolder {

  private val emptyRemoteObjects = RemoteObjects.empty

  "create a plan" - {

    "one source" - {
      val options: Path => ConfigOptions =
        source =>
          configOptions(ConfigOption.Source(source),
                        ConfigOption.Bucket("a-bucket"),
                        ConfigOption.IgnoreUserOptions,
                        ConfigOption.IgnoreGlobalOptions)
      "a file" - {
        val filename  = "aFile"
        val remoteKey = RemoteKey(filename)
        "with no matching remote key" - {
          "with no other remote key with matching hash" - {
            "upload file" in {
              withDirectory(source => {
                val file = createFile(source, filename, "file-content")
                val hash = md5Hash(file)
                val expected =
                  Right(List(toUpload(remoteKey, hash, source, file)))
                val result =
                  invoke(options(source),
                         UIO.succeed(emptyRemoteObjects),
                         UIO.succeed(Map(file.toPath -> file)))
                assertResult(expected)(result)
              })
            }
          }
          "with another remote key with matching hash" - {
            "copy file" in {
              withDirectory(source => {
                val anOtherFilename = "other"
                val content         = "file-content"
                val aFile           = createFile(source, filename, content)
                val anOtherFile     = createFile(source, anOtherFilename, content)
                val aHash           = md5Hash(aFile)
                val anOtherKey      = RemoteKey("other")
                val expected        = Right(List(toCopy(anOtherKey, aHash, remoteKey)))
                val remoteObjects = RemoteObjects(
                  byHash = Map(aHash     -> Set(anOtherKey)),
                  byKey = Map(anOtherKey -> aHash)
                )
                val result =
                  invoke(options(source),
                         UIO.succeed(remoteObjects),
                         UIO.succeed(Map(aFile.toPath       -> aFile,
                                         anOtherFile.toPath -> anOtherFile)))
                assertResult(expected)(result)
              })
            }
          }
        }
        "with matching remote key" - {
          "with matching hash" - {
            "do nothing" in {
              withDirectory(source => {
                val file = createFile(source, filename, "file-content")
                val hash = md5Hash(file)
                // DoNothing actions should have been filtered out of the plan
                val expected = Right(List())
                val remoteObjects = RemoteObjects(
                  byHash = Map(hash     -> Set(remoteKey)),
                  byKey = Map(remoteKey -> hash)
                )
                val result =
                  invoke(options(source),
                         UIO.succeed(remoteObjects),
                         UIO.succeed(Map(file.toPath -> file)))
                assertResult(expected)(result)
              })
            }
          }
          "with different hash" - {
            "with no matching remote hash" - {
              "upload file" in {
                withDirectory(source => {
                  val file         = createFile(source, filename, "file-content")
                  val currentHash  = md5Hash(file)
                  val originalHash = MD5Hash("original-file-content")
                  val expected =
                    Right(List(toUpload(remoteKey, currentHash, source, file)))
                  val remoteObjects = RemoteObjects(
                    byHash = Map(originalHash -> Set(remoteKey)),
                    byKey = Map(remoteKey     -> originalHash)
                  )
                  val result =
                    invoke(options(source),
                           UIO.succeed(remoteObjects),
                           UIO.succeed(Map(file.toPath -> file)))
                  assertResult(expected)(result)
                })
              }
            }
            "with matching remote hash" - {
              "copy file" in {
                withDirectory(source => {
                  val file      = createFile(source, filename, "file-content")
                  val hash      = md5Hash(file)
                  val sourceKey = RemoteKey("other-key")
                  val expected  = Right(List(toCopy(sourceKey, hash, remoteKey)))
                  val remoteObjects = RemoteObjects(
                    byHash = Map(hash -> Set(sourceKey)),
                    byKey = Map.empty
                  )
                  val result =
                    invoke(options(source),
                           UIO.succeed(remoteObjects),
                           UIO.succeed(Map(file.toPath -> file)))
                  assertResult(expected)(result)
                })
              }
            }
          }
        }
      }
      "a remote key" - {
        val filename  = "aFile"
        val remoteKey = RemoteKey(filename)
        "with a matching local file" - {
          "do nothing" in {
            withDirectory(source => {
              val file = createFile(source, filename, "file-content")
              val hash = md5Hash(file)
              // DoNothing actions should have been filtered out of the plan
              val expected = Right(List())
              val remoteObjects = RemoteObjects(
                byHash = Map(hash     -> Set(remoteKey)),
                byKey = Map(remoteKey -> hash)
              )
              val result =
                invoke(options(source),
                       UIO.succeed(remoteObjects),
                       UIO.succeed(Map(file.toPath -> file)))
              assertResult(expected)(result)
            })
          }
        }
        "with no matching local file" - {
          "delete remote key" in {
            withDirectory(source => {
              val hash     = MD5Hash("file-content")
              val expected = Right(List(toDelete(remoteKey)))
              val remoteObjects = RemoteObjects(
                byHash = Map(hash     -> Set(remoteKey)),
                byKey = Map(remoteKey -> hash)
              )
              val result =
                invoke(options(source),
                       UIO.succeed(remoteObjects),
                       UIO.succeed(Map.empty))
              assertResult(expected)(result)
            })
          }
        }
      }
    }

    "two sources" - {
      val filename1  = "file-1"
      val filename2  = "file-2"
      val remoteKey1 = RemoteKey(filename1)
      val remoteKey2 = RemoteKey(filename2)
      val options: Path => Path => ConfigOptions =
        source1 =>
          source2 =>
            configOptions(ConfigOption.Source(source1),
                          ConfigOption.Source(source2),
                          ConfigOption.Bucket("a-bucket"))
      "unique files in both" - {
        "upload all files" in {
          withDirectory(firstSource => {
            val fileInFirstSource =
              createFile(firstSource, filename1, "file-1-content")
            val hash1 = md5Hash(fileInFirstSource)
            withDirectory(secondSource => {
              val fileInSecondSource =
                createFile(secondSource, filename2, "file-2-content")
              val hash2 = md5Hash(fileInSecondSource)
              val expected = Right(
                Set(
                  toUpload(remoteKey2, hash2, secondSource, fileInSecondSource),
                  toUpload(remoteKey1, hash1, firstSource, fileInFirstSource)
                ))
              val result =
                invoke(
                  options(firstSource)(secondSource),
                  UIO.succeed(emptyRemoteObjects),
                  UIO.succeed(
                    Map(fileInFirstSource.toPath  -> fileInFirstSource,
                        fileInSecondSource.toPath -> fileInSecondSource))
                ).map(_.toSet)
              assertResult(expected)(result)
            })
          })
        }
      }
      "same filename in both" - {
        "only upload file in first source" in {
          withDirectory(firstSource => {
            val fileInFirstSource =
              createFile(firstSource, filename1, "file-1-content")
            val hash1 = md5Hash(fileInFirstSource)
            withDirectory(secondSource => {
              val fileInSecondSource =
                createFile(secondSource, filename1, "file-2-content")
              val hash2 = md5Hash(fileInSecondSource)
              val expected = Right(List(
                toUpload(remoteKey1, hash1, firstSource, fileInFirstSource)))
              val result =
                invoke(
                  options(firstSource)(secondSource),
                  UIO.succeed(emptyRemoteObjects),
                  UIO.succeed(
                    Map(fileInFirstSource.toPath  -> fileInFirstSource,
                        fileInSecondSource.toPath -> fileInSecondSource))
                )
              assertResult(expected)(result)
            })
          })
        }
      }
      "with a remote file only present in second source" - {
        "do not delete it " in {
          withDirectory(firstSource => {
            withDirectory(secondSource => {
              val fileInSecondSource =
                createFile(secondSource, filename2, "file-2-content")
              val hash2    = md5Hash(fileInSecondSource)
              val expected = Right(List())
              val remoteObjects =
                RemoteObjects(byHash = Map(hash2     -> Set(remoteKey2)),
                              byKey = Map(remoteKey2 -> hash2))
              val result =
                invoke(options(firstSource)(secondSource),
                       UIO.succeed(remoteObjects),
                       UIO.succeed(
                         Map(fileInSecondSource.toPath -> fileInSecondSource)))
              assertResult(expected)(result)
            })
          })
        }
      }
      "with remote file only present in first source" - {
        "do not delete it" in {
          withDirectory(firstSource => {
            val fileInFirstSource: File =
              createFile(firstSource, filename1, "file-1-content")
            val hash1 = md5Hash(fileInFirstSource)
            withDirectory(secondSource => {
              val expected = Right(List())
              val remoteObjects =
                RemoteObjects(byHash = Map(hash1     -> Set(remoteKey1)),
                              byKey = Map(remoteKey1 -> hash1))
              val result =
                invoke(options(firstSource)(secondSource),
                       UIO.succeed(remoteObjects),
                       UIO.succeed(
                         Map(fileInFirstSource.toPath -> fileInFirstSource)))
              assertResult(expected)(result)
            })
          })
        }
      }
      "with remote file not present in either source" - {
        "delete from remote" in {
          withDirectory(firstSource => {
            withDirectory(secondSource => {
              val expected = Right(List(toDelete(remoteKey1)))
              val remoteObjects =
                RemoteObjects(byHash = Map.empty,
                              byKey = Map(remoteKey1 -> MD5Hash("")))
              val result =
                invoke(options(firstSource)(secondSource),
                       UIO.succeed(remoteObjects),
                       UIO.succeed(Map.empty))
              assertResult(expected)(result)
            })
          })
        }
      }
    }

    def md5Hash(file: File): MD5Hash = {
      object TestEnv extends Hasher.Live with FileSystem.Live
      new DefaultRuntime {}
        .unsafeRunSync {
          Hasher
            .hashObject(file.toPath)
            .map(_.get(MD5))
            .provide(TestEnv)
        }
        .toEither
        .toOption
        .flatten
        .getOrElse(MD5Hash("invalid md5 hash in test"))
    }

  }

  private def toUpload(remoteKey: RemoteKey,
                       md5Hash: MD5Hash,
                       source: Path,
                       file: File): (String, String, String, String, String) =
    ("upload",
     remoteKey.key,
     MD5Hash.hash(md5Hash),
     source.toFile.getPath,
     file.toString)

  private def toCopy(
      sourceKey: RemoteKey,
      md5Hash: MD5Hash,
      targetKey: RemoteKey): (String, String, String, String, String) =
    ("copy", sourceKey.key, MD5Hash.hash(md5Hash), targetKey.key, "")

  private def toDelete(
      remoteKey: RemoteKey): (String, String, String, String, String) =
    ("delete", remoteKey.key, "", "", "")

  private def configOptions(configOptions: ConfigOption*): ConfigOptions =
    ConfigOptions(List(configOptions: _*))

  private def invoke(
      configOptions: ConfigOptions,
      result: Task[RemoteObjects],
      files: Task[Map[Path, File]]
  ) = {
    type TestEnv = Storage with Console with Config with FileSystem with Hasher
    val testEnv: TestEnv = new Storage.Test with Console.Test with Config.Live
    with FileSystem.Live with Hasher.Live {
      override def listResult: Task[RemoteObjects] = result
      override def uploadResult: UIO[StorageQueueEvent] =
        Task.die(new NotImplementedError)
      override def copyResult: UIO[StorageQueueEvent] =
        Task.die(new NotImplementedError)
      override def deleteResult: UIO[StorageQueueEvent] =
        Task.die(new NotImplementedError)
      override def shutdownResult: UIO[StorageQueueEvent] =
        Task.die(new NotImplementedError)
    }
    def testProgram =
      for {
        config <- ConfigurationBuilder.buildConfig(configOptions)
        _      <- Config.set(config)
        plan   <- PlanBuilder.createPlan
      } yield plan
    new DefaultRuntime {}
      .unsafeRunSync(testProgram.provide(testEnv))
      .toEither
      .map(convertResult)
  }

  private def convertResult(plan: SyncPlan) =
    plan.actions.map({
      case ToUpload(_, lf, _) =>
        ("upload",
         lf.remoteKey.key,
         MD5Hash.hash(lf.hashes(MD5)),
         lf.source.toString,
         lf.file.toString)
      case ToDelete(_, remoteKey, _) => ("delete", remoteKey.key, "", "", "")
      case ToCopy(_, sourceKey, hash, targetKey, _) =>
        ("copy", sourceKey.key, MD5Hash.hash(hash), targetKey.key, "")
      case DoNothing(_, remoteKey, _) =>
        ("do-nothing", remoteKey.key, "", "", "")
      case _ => ("other", "", "", "", "")
    })
}
