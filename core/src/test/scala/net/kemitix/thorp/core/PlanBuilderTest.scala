package net.kemitix.thorp.core

import java.io.File
import java.nio.file.Path

import net.kemitix.thorp.core.Action.{ToDelete, ToUpload}
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.{HashService, StorageService}
import org.scalatest.FreeSpec

class PlanBuilderTest extends FreeSpec with TemporaryFolder {

  private val planBuilder = new PlanBuilder {}
  private val emptyS3ObjectData = S3ObjectsData()
  private implicit val logger: Logger = new DummyLogger

  val lastModified: LastModified = LastModified()

  "create a plan" - {

    val filename1 = "file-1"
    val filename2 = "file-2"
    val remoteKey1 = RemoteKey(filename1)
    val remoteKey2 = RemoteKey(filename2)
    val hashService = SimpleHashService()

    "two sources" - {
      "unique files in both" - {
        "upload all files" in {
          withDirectory(firstSource => {
            val fileInFirstSource = createFile(firstSource, filename1, "file-1-content")
            val hash1 = hashService.hashLocalObject(fileInFirstSource.toPath).unsafeRunSync()("md5")

            withDirectory(secondSource => {
              val fileInSecondSource = createFile(secondSource, filename2, "file-2-content")
              val hash2 = hashService.hashLocalObject(fileInSecondSource.toPath).unsafeRunSync()("md5")

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
            val hash1 = hashService.hashLocalObject(fileInFirstSource.toPath).unsafeRunSync()("md5")

            withDirectory(secondSource => {
              val fileInSecondSource: File = createFile(secondSource, filename1, "file-2-content")
              val hash2 = hashService.hashLocalObject(fileInSecondSource.toPath).unsafeRunSync()("md5")

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
              val hash2 = hashService.hashLocalObject(fileInSecondSource.toPath).unsafeRunSync()("md5")

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
            val hash1 = hashService.hashLocalObject(fileInFirstSource.toPath).unsafeRunSync()("md5")

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

              val expected = Right(List(("delete", remoteKey1.key, "", "", "")))

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
  }

  private def toUpload(remoteKey: RemoteKey,
                       md5Hash: MD5Hash,
                       source: Path,
                       file: File): (String, String, String, String, String) =
    ("upload", remoteKey.key, md5Hash.hash, source.toString, file.toString)

  private def configOptions(configOptions: ConfigOption*): ConfigOptions =
    ConfigOptions(List(configOptions:_*))

  private def invoke(storageService: StorageService,
                     hashService: HashService,
                     configOptions: ConfigOptions): Either[List[String], List[(String, String, String, String, String)]] =
    planBuilder.createPlan(storageService, hashService, configOptions)
      .value.unsafeRunSync().map(_.actions.toList.map({
      case ToUpload(_, lf, _) => ("upload", lf.remoteKey.key, lf.hashes("md5").hash, lf.source.toString, lf.file.toString)
      case ToDelete(_, remoteKey, _) => ("delete", remoteKey.key, "", "", "")
      case x => ("other", x.toString, "", "", "")
    }))

}
