package net.kemitix.thorp.lib

import net.kemitix.thorp.config._
import net.kemitix.thorp.domain.Action.{DoNothing, ToCopy, ToUpload}
import net.kemitix.thorp.domain.HashType.MD5
import net.kemitix.thorp.domain._
import net.kemitix.thorp.filesystem.{FileSystem, Resource}
import org.scalatest.FunSpec
import zio.DefaultRuntime

class ActionGeneratorSuite extends FunSpec {
  private val source     = Resource(this, "upload")
  private val sourcePath = source.toPath
  private val sources    = Sources(List(sourcePath))
  private val prefix     = RemoteKey("prefix")
  private val bucket     = Bucket("bucket")
  private val configOptions = ConfigOptions(
    List[ConfigOption](
      ConfigOption.Bucket("bucket"),
      ConfigOption.Prefix("prefix"),
      ConfigOption.Source(sourcePath),
      ConfigOption.IgnoreUserOptions,
      ConfigOption.IgnoreGlobalOptions
    ))

  describe("create actions") {

    val previousActions = LazyList.empty[Action]

    describe("#1 local exists, remote exists, remote matches - do nothing") {
      val theHash = MD5Hash("the-hash")
      val env = for {
        theFile <- LocalFileValidator.resolve("the-file",
                                              md5HashMap(theHash),
                                              sourcePath,
                                              sources,
                                              prefix)
        theRemoteMetadata = RemoteMetaData(theFile.remoteKey, theHash)
        input = MatchedMetadata(
          theFile, // local exists
          matchByHash = Some(theRemoteMetadata), // remote matches
          matchByKey = Some(theRemoteMetadata) // remote exists
        )
      } yield (theFile, input)
      it("do nothing") {
        env.map({
          case (theFile, input) => {
            val expected =
              Right(LazyList(
                DoNothing(bucket, theFile.remoteKey, theFile.file.length + 1)))
            val result = invoke(input, previousActions)
            assertResult(expected)(result)
          }
        })
      }
    }
    describe("#2 local exists, remote is missing, other matches - copy") {
      val theHash = MD5Hash("the-hash")
      val env = for {
        theFile <- LocalFileValidator.resolve("the-file",
                                              md5HashMap(theHash),
                                              sourcePath,
                                              sources,
                                              prefix)
        theRemoteKey        = theFile.remoteKey
        otherRemoteKey      = RemoteKey.resolve("other-key")(prefix)
        otherRemoteMetadata = RemoteMetaData(otherRemoteKey, theHash)
        input = MatchedMetadata(
          theFile, // local exists
          matchByHash = Some(otherRemoteMetadata), // other matches
          matchByKey = None) // remote is missing
      } yield (theFile, theRemoteKey, input, otherRemoteKey)
      it("copy from other key") {
        env.map({
          case (theFile, theRemoteKey, input, otherRemoteKey) => {
            val expected = Right(
              LazyList(
                ToCopy(bucket,
                       otherRemoteKey,
                       theHash,
                       theRemoteKey,
                       theFile.file.length))) // copy
            val result = invoke(input, previousActions)
            assertResult(expected)(result)
          }
        })
      }
      describe("#3 local exists, remote is missing, other no matches - upload") {
        val theHash = MD5Hash("the-hash")
        val env = for {
          theFile <- LocalFileValidator.resolve("the-file",
                                                md5HashMap(theHash),
                                                sourcePath,
                                                sources,
                                                prefix)
          input = MatchedMetadata(theFile, // local exists
                                  matchByHash = None, // other no matches
                                  matchByKey = None) // remote is missing
        } yield (theFile, input)
        it("upload") {
          env.map({
            case (theFile, input) => {
              val expected = Right(LazyList(
                ToUpload(bucket, theFile, theFile.file.length))) // upload
              val result = invoke(input, previousActions)
              assertResult(expected)(result)
            }
          })
        }
      }
    }
    describe(
      "#4 local exists, remote exists, remote no match, other matches - copy") {
      val theHash = MD5Hash("the-hash")
      val env = for {
        theFile <- LocalFileValidator.resolve("the-file",
                                              md5HashMap(theHash),
                                              sourcePath,
                                              sources,
                                              prefix)
        theRemoteKey        = theFile.remoteKey
        oldHash             = MD5Hash("old-hash")
        otherRemoteKey      = RemoteKey.resolve("other-key")(prefix)
        otherRemoteMetadata = RemoteMetaData(otherRemoteKey, theHash)
        oldRemoteMetadata = RemoteMetaData(theRemoteKey,
                                           hash = oldHash // remote no match
        )
        input = MatchedMetadata(
          theFile, // local exists
          matchByHash = Some(otherRemoteMetadata), // other matches
          matchByKey = Some(oldRemoteMetadata)) // remote exists
      } yield (theFile, theRemoteKey, input, otherRemoteKey)
      it("copy from other key") {
        env.map({
          case (theFile, theRemoteKey, input, otherRemoteKey) => {
            val expected = Right(
              LazyList(
                ToCopy(bucket,
                       otherRemoteKey,
                       theHash,
                       theRemoteKey,
                       theFile.file.length))) // copy
            val result = invoke(input, previousActions)
            assertResult(expected)(result)
          }
        })
      }
    }
    describe(
      "#5 local exists, remote exists, remote no match, other no matches - upload") {
      val theHash = MD5Hash("the-hash")
      val env = for {
        theFile <- LocalFileValidator.resolve("the-file",
                                              md5HashMap(theHash),
                                              sourcePath,
                                              sources,
                                              prefix)
        theRemoteKey      = theFile.remoteKey
        oldHash           = MD5Hash("old-hash")
        theRemoteMetadata = RemoteMetaData(theRemoteKey, oldHash)
        input = MatchedMetadata(
          theFile, // local exists
          matchByHash = None, // remote no match, other no match
          matchByKey = Some(theRemoteMetadata) // remote exists
        )
      } yield (theFile, input)
      it("upload") {
        env.map({
          case (theFile, input) => {
            val expected = Right(LazyList(
              ToUpload(bucket, theFile, theFile.file.length))) // upload
            val result = invoke(input, previousActions)
            assertResult(expected)(result)
          }
        })
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
      input: MatchedMetadata,
      previousActions: LazyList[Action]
  ) = {
    type TestEnv = Config with FileSystem
    val testEnv: TestEnv = new Config.Live with FileSystem.Live {}

    def testProgram =
      for {
        config  <- ConfigurationBuilder.buildConfig(configOptions)
        _       <- Config.set(config)
        actions <- ActionGenerator.createActions(input, previousActions)
      } yield actions

    new DefaultRuntime {}.unsafeRunSync {
      testProgram.provide(testEnv)
    }.toEither
  }
}
