package net.kemitix.thorp.core

import java.io.File
import java.nio.file.Path

import net.kemitix.thorp.core.Action.{DoNothing, ToCopy, ToDelete, ToUpload}
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.{HashService, StorageService}
import org.scalatest.FreeSpec

class PlanBuilderTest extends FreeSpec with TemporaryFolder {

  private val planBuilder = new PlanBuilder {}
  private val emptyS3ObjectData = S3ObjectsData()
  private implicit val logger: Logger = new DummyLogger

  val lastModified: LastModified = LastModified()

  "create a plan" - {

    val hashService = SimpleHashService()

    "one source" - {
      "a file" - {
        val filename = "aFile"
        val remoteKey = RemoteKey(filename)
        "with no matching remote key" - {
          "with no other remote key with matching hash" - {
            "upload file" in {
              withDirectory(source => {
                val file = createFile(source, filename, "file-content")
                val hash = md5Hash(file)

                val expected = Right(List(
                  toUpload(remoteKey, hash, source, file)
                ))

                val storageService = DummyStorageService(emptyS3ObjectData, Map(
                  file -> (remoteKey, hash)
                ))

                val result = invoke(storageService, hashService, configOptions(
                  ConfigOption.Source(source),
                  ConfigOption.Bucket("a-bucket")))

                assertResult(expected)(result)
              })
            }
          }
          "with another remote key with matching hash" - {
            "copy file" in {
              withDirectory(source => {
                val anOtherFilename = "other"
                val content = "file-content"
                val aFile = createFile(source, filename, content)
                val anOtherFile = createFile(source, anOtherFilename, content)
                val aHash = md5Hash(aFile)

                val anOtherKey = RemoteKey("other")

                val expected = Right(List(
                  toCopy(anOtherKey, aHash, remoteKey)
                ))

                val s3ObjectsData = S3ObjectsData(
                  byHash = Map(aHash -> Set(KeyModified(anOtherKey, lastModified))),
                  byKey = Map(anOtherKey -> HashModified(aHash, lastModified))
                )

                val storageService = DummyStorageService(s3ObjectsData, Map(
                  aFile -> (remoteKey, aHash)
                ))

                val result = invoke(storageService, hashService, configOptions(
                  ConfigOption.Source(source),
                  ConfigOption.Bucket("a-bucket")))

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

                val s3ObjectsData = S3ObjectsData(
                  byHash = Map(hash -> Set(KeyModified(remoteKey, lastModified))),
                  byKey = Map(remoteKey -> HashModified(hash, lastModified))
                )

                val storageService = DummyStorageService(s3ObjectsData, Map(
                  file -> (remoteKey, hash)
                ))

                val result = invoke(storageService, hashService, configOptions(
                  ConfigOption.Source(source),
                  ConfigOption.Bucket("a-bucket")))

                assertResult(expected)(result)
              })
            }
          }
          "with no other remote key with matching hash" - {
            "upload file" ignore {}
          }
          "with another remote key with matching hash" - {
            "copy file" ignore {}
          }
        }
      }
      "a remote key" - {
        "with a matching local file" - {
          "do nothing" ignore {}
        }
        "with no matching local file" - {
          "delete remote key" ignore {}
        }
      }
    }

    "two sources" - {
      val filename1 = "file-1"
      val filename2 = "file-2"
      val remoteKey1 = RemoteKey(filename1)
      val remoteKey2 = RemoteKey(filename2)
      "unique files in both" - {
        "upload all files" in {
          withDirectory(firstSource => {
            val fileInFirstSource = createFile(firstSource, filename1, "file-1-content")
            val hash1 = md5Hash(fileInFirstSource)

            withDirectory(secondSource => {
              val fileInSecondSource = createFile(secondSource, filename2, "file-2-content")
              val hash2 = md5Hash(fileInSecondSource)

              val expected = Right(List(
                toUpload(remoteKey2, hash2, secondSource, fileInSecondSource),
                toUpload(remoteKey1, hash1, firstSource, fileInFirstSource)
              ))

              val storageService = DummyStorageService(emptyS3ObjectData, Map(
                fileInFirstSource -> (remoteKey1, hash1),
                fileInSecondSource -> (remoteKey2, hash2)))

              val result = invoke(storageService, hashService, configOptions(
                ConfigOption.Source(firstSource),
                ConfigOption.Source(secondSource),
                ConfigOption.Bucket("a-bucket")))

              assertResult(expected)(result)
            })
          })
        }
      }
      "same filename in both" - {
        "only upload file in first source" in {
          withDirectory(firstSource => {
            val fileInFirstSource: File = createFile(firstSource, filename1, "file-1-content")
            val hash1 = md5Hash(fileInFirstSource)

            withDirectory(secondSource => {
              val fileInSecondSource: File = createFile(secondSource, filename1, "file-2-content")
              val hash2 = md5Hash(fileInSecondSource)

              val expected = Right(List(
                toUpload(remoteKey1, hash1, firstSource, fileInFirstSource)
              ))

              val storageService = DummyStorageService(emptyS3ObjectData, Map(
                fileInFirstSource -> (remoteKey1, hash1),
                fileInSecondSource -> (remoteKey2, hash2)))

              val result = invoke(storageService, hashService, configOptions(
                ConfigOption.Source(firstSource),
                ConfigOption.Source(secondSource),
                ConfigOption.Bucket("a-bucket")))

              assertResult(expected)(result)
            })
          })
        }
      }
      "with a remote file only present in second source" -  {
        "do not delete it " in {
          withDirectory(firstSource => {

            withDirectory(secondSource => {
              val fileInSecondSource = createFile(secondSource, filename2, "file-2-content")
              val hash2 = md5Hash(fileInSecondSource)

              val expected = Right(List())

              val s3ObjectData = S3ObjectsData(
                byHash = Map(hash2 -> Set(KeyModified(remoteKey2, lastModified))),
                byKey = Map(remoteKey2 -> HashModified(hash2, lastModified)))

              val storageService = DummyStorageService(s3ObjectData, Map(
                fileInSecondSource -> (remoteKey2, hash2)))

              val result = invoke(storageService, hashService, configOptions(
                ConfigOption.Source(firstSource),
                ConfigOption.Source(secondSource),
                ConfigOption.Bucket("a-bucket")))

              assertResult(expected)(result)
            })
          })
        }
      }
      "with remote file only present in first source" - {
        "do not delete it" in {
          withDirectory(firstSource => {
            val fileInFirstSource: File = createFile(firstSource, filename1, "file-1-content")
            val hash1 = md5Hash(fileInFirstSource)

            withDirectory(secondSource => {

              val expected = Right(List())

              val s3ObjectData = S3ObjectsData(
                byHash = Map(hash1 -> Set(KeyModified(remoteKey1, lastModified))),
                byKey = Map(remoteKey1 -> HashModified(hash1, lastModified)))

              val storageService = DummyStorageService(s3ObjectData, Map(
                fileInFirstSource -> (remoteKey1, hash1)))

              val result = invoke(storageService, hashService, configOptions(
                ConfigOption.Source(firstSource),
                ConfigOption.Source(secondSource),
                ConfigOption.Bucket("a-bucket")))

              assertResult(expected)(result)
            })
          })
        }
      }
      "with remote file not present in either source" - {
        "delete from remote" in {
          withDirectory(firstSource => {

            withDirectory(secondSource => {

              val expected = Right(List(
                toDelete(remoteKey1)
              ))

              val s3ObjectData = S3ObjectsData(
                byKey = Map(remoteKey1 -> HashModified(MD5Hash(""), lastModified)))

              val storageService = DummyStorageService(s3ObjectData, Map())

              val result = invoke(storageService, hashService, configOptions(
                ConfigOption.Source(firstSource),
                ConfigOption.Source(secondSource),
                ConfigOption.Bucket("a-bucket")))

              assertResult(expected)(result)
            })
          })
        }
      }
    }

    def md5Hash(file: File) = {
      hashService.hashLocalObject(file.toPath).unsafeRunSync()("md5")
    }

  }

  private def toUpload(remoteKey: RemoteKey,
                       md5Hash: MD5Hash,
                       source: Path,
                       file: File): (String, String, String, String, String) =
    ("upload", remoteKey.key, md5Hash.hash, source.toString, file.toString)

  private def toCopy(sourceKey: RemoteKey,
                     md5Hash: MD5Hash,
                     targetKey: RemoteKey): (String, String, String, String, String) =
    ("copy", sourceKey.key, md5Hash.hash, targetKey.key, "")

  private def toDelete(remoteKey: RemoteKey): (String, String, String, String, String) =
    ("delete", remoteKey.key, "", "", "")

  private def configOptions(configOptions: ConfigOption*): ConfigOptions =
    ConfigOptions(List(configOptions:_*))

  private def invoke(storageService: StorageService,
                     hashService: HashService,
                     configOptions: ConfigOptions): Either[List[String], List[(String, String, String, String, String)]] =
    planBuilder.createPlan(storageService, hashService, configOptions)
      .value.unsafeRunSync().map(_.actions.toList.map({
      case ToUpload(_, lf, _) => ("upload", lf.remoteKey.key, lf.hashes("md5").hash, lf.source.toString, lf.file.toString)
      case ToDelete(_, remoteKey, _) => ("delete", remoteKey.key, "", "", "")
      case ToCopy(_, sourceKey, hash, targetKey, _) => ("copy", sourceKey.key, hash.hash, targetKey.key, "")
      case DoNothing(_, remoteKey, _) => ("do-nothing", remoteKey.key, "", "", "")
      case x => ("other", x.toString, "", "", "")
    }))

}
