package net.kemitix.thorp.core

import java.time.Instant

import net.kemitix.thorp.config._
import net.kemitix.thorp.core.Action.{DoNothing, ToCopy, ToUpload}
import net.kemitix.thorp.domain.HashType.MD5
import net.kemitix.thorp.domain._
import org.scalatest.FunSpec
import zio.DefaultRuntime

class ActionGeneratorSuite extends FunSpec {
  val lastModified       = LastModified(Instant.now())
  private val source     = Resource(this, "upload")
  private val sourcePath = source.toPath
  private val sources    = Sources(List(sourcePath))
  private val prefix     = RemoteKey("prefix")
  private val bucket     = Bucket("bucket")
  private val configOptions = ConfigOptions(
    List(
      ConfigOption.Bucket("bucket"),
      ConfigOption.Prefix("prefix"),
      ConfigOption.Source(sourcePath),
      ConfigOption.IgnoreUserOptions,
      ConfigOption.IgnoreGlobalOptions
    ))
  private val fileToKey =
    KeyGenerator.generateKey(sources, prefix) _

  describe("create actions") {

    val previousActions = Stream.empty[Action]

    describe("#1 local exists, remote exists, remote matches - do nothing") {
      val theHash = MD5Hash("the-hash")
      val theFile = LocalFile.resolve("the-file",
                                      md5HashMap(theHash),
                                      sourcePath,
                                      fileToKey)
      val theRemoteMetadata =
        RemoteMetaData(theFile.remoteKey, theHash, lastModified)
      val input =
        S3MetaData(theFile, // local exists
                   matchByHash = Set(theRemoteMetadata), // remote matches
                   matchByKey = Some(theRemoteMetadata) // remote exists
        )
      it("do nothing") {
        val expected =
          Right(
            Stream(DoNothing(bucket, theFile.remoteKey, theFile.file.length)))
        val result = invoke(input, previousActions)
        assertResult(expected)(result)
      }
    }
    describe("#2 local exists, remote is missing, other matches - copy") {
      val theHash = MD5Hash("the-hash")
      val theFile = LocalFile.resolve("the-file",
                                      md5HashMap(theHash),
                                      sourcePath,
                                      fileToKey)
      val theRemoteKey   = theFile.remoteKey
      val otherRemoteKey = prefix.resolve("other-key")
      val otherRemoteMetadata =
        RemoteMetaData(otherRemoteKey, theHash, lastModified)
      val input =
        S3MetaData(theFile, // local exists
                   matchByHash = Set(otherRemoteMetadata), // other matches
                   matchByKey = None) // remote is missing
      it("copy from other key") {
        val expected = Right(
          Stream(
            ToCopy(bucket,
                   otherRemoteKey,
                   theHash,
                   theRemoteKey,
                   theFile.file.length))) // copy
        val result = invoke(input, previousActions)
        assertResult(expected)(result)
      }
    }
    describe("#3 local exists, remote is missing, other no matches - upload") {
      val theHash = MD5Hash("the-hash")
      val theFile = LocalFile.resolve("the-file",
                                      md5HashMap(theHash),
                                      sourcePath,
                                      fileToKey)
      val input = S3MetaData(theFile, // local exists
                             matchByHash = Set.empty, // other no matches
                             matchByKey = None) // remote is missing
      it("upload") {
        val expected = Right(
          Stream(ToUpload(bucket, theFile, theFile.file.length))) // upload
        val result = invoke(input, previousActions)
        assertResult(expected)(result)
      }
    }
    describe(
      "#4 local exists, remote exists, remote no match, other matches - copy") {
      val theHash = MD5Hash("the-hash")
      val theFile = LocalFile.resolve("the-file",
                                      md5HashMap(theHash),
                                      sourcePath,
                                      fileToKey)
      val theRemoteKey   = theFile.remoteKey
      val oldHash        = MD5Hash("old-hash")
      val otherRemoteKey = prefix.resolve("other-key")
      val otherRemoteMetadata =
        RemoteMetaData(otherRemoteKey, theHash, lastModified)
      val oldRemoteMetadata = RemoteMetaData(theRemoteKey,
                                             hash = oldHash, // remote no match
                                             lastModified = lastModified)
      val input =
        S3MetaData(theFile, // local exists
                   matchByHash = Set(otherRemoteMetadata), // other matches
                   matchByKey = Some(oldRemoteMetadata)) // remote exists
      it("copy from other key") {
        val expected = Right(
          Stream(
            ToCopy(bucket,
                   otherRemoteKey,
                   theHash,
                   theRemoteKey,
                   theFile.file.length))) // copy
        val result = invoke(input, previousActions)
        assertResult(expected)(result)
      }
    }
    describe(
      "#5 local exists, remote exists, remote no match, other no matches - upload") {
      val theHash = MD5Hash("the-hash")
      val theFile = LocalFile.resolve("the-file",
                                      md5HashMap(theHash),
                                      sourcePath,
                                      fileToKey)
      val theRemoteKey = theFile.remoteKey
      val oldHash      = MD5Hash("old-hash")
      val theRemoteMetadata =
        RemoteMetaData(theRemoteKey, oldHash, lastModified)
      val input =
        S3MetaData(theFile, // local exists
                   matchByHash = Set.empty, // remote no match, other no match
                   matchByKey = Some(theRemoteMetadata) // remote exists
        )
      it("upload") {
        val expected = Right(
          Stream(ToUpload(bucket, theFile, theFile.file.length))) // upload
        val result = invoke(input, previousActions)
        assertResult(expected)(result)
      }
    }
    describe("#6 local missing, remote exists - delete") {
      it("TODO") {
        pending
      }
    }
  }

  private def md5HashMap(theHash: MD5Hash): Map[HashType, MD5Hash] = {
    Map(MD5 -> theHash)
  }

  private def invoke(
      input: S3MetaData,
      previousActions: Stream[Action]
  ) = {
    type TestEnv = Config
    val testEnv: TestEnv = new Config.Live {}

    def testProgram =
      for {
        config  <- ConfigurationBuilder.buildConfig(configOptions)
        _       <- setConfig(config)
        actions <- ActionGenerator.createActions(input, previousActions)
      } yield actions

    new DefaultRuntime {}.unsafeRunSync {
      testProgram.provide(testEnv)
    }.toEither
  }
}
