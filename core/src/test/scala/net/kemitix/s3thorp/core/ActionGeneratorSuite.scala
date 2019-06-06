package net.kemitix.s3thorp.core

import java.io.File
import java.time.Instant

import net.kemitix.s3thorp.core.Action.{DoNothing, ToCopy, ToUpload}
import net.kemitix.s3thorp.domain._
import org.scalatest.FunSpec

class ActionGeneratorSuite
  extends FunSpec {

  private val source = Resource(this, "upload")
  private val prefix = RemoteKey("prefix")
  private val bucket = Bucket("bucket")
  implicit private val config: Config = Config(bucket, prefix, source = source)
  implicit private val logInfo: Int => String => Unit = l => i => ()
  private val fileToKey = KeyGenerator.generateKey(config.source, config.prefix) _
  private val fileToHash = (file: File) => MD5HashGenerator.md5File(file)
  val lastModified = LastModified(Instant.now())

    describe("create actions") {

      def invoke(input: S3MetaData) = ActionGenerator.createActions(input).toList

      describe("#1 local exists, remote exists, remote matches - do nothing") {
        val theHash = MD5Hash("the-hash")
        val theFile = LocalFile.resolve("the-file", theHash, source, fileToKey, fileToHash)
        val theRemoteMetadata = RemoteMetaData(theFile.remoteKey, theHash, lastModified)
        val input = S3MetaData(theFile, // local exists
          matchByHash = Set(theRemoteMetadata), // remote matches
          matchByKey = Some(theRemoteMetadata) // remote exists
          )
        it("do nothing") {
          val expected = List(DoNothing(bucket, theFile.remoteKey))
          val result = invoke(input)
          assertResult(expected)(result)
        }
      }
      describe("#2 local exists, remote is missing, other matches - copy") {
        val theHash = MD5Hash("the-hash")
        val theFile = LocalFile.resolve("the-file", theHash, source, fileToKey, fileToHash)
        val theRemoteKey = theFile.remoteKey
        val otherRemoteKey = prefix.resolve("other-key")
        val otherRemoteMetadata = RemoteMetaData(otherRemoteKey, theHash, lastModified)
        val input = S3MetaData(theFile, // local exists
          matchByHash = Set(otherRemoteMetadata), // other matches
          matchByKey = None) // remote is missing
        it("copy from other key") {
          val expected = List(ToCopy(bucket, otherRemoteKey, theHash, theRemoteKey)) // copy
          val result = invoke(input)
          assertResult(expected)(result)
        }
      }
      describe("#3 local exists, remote is missing, other no matches - upload") {
        val theHash = MD5Hash("the-hash")
        val theFile = LocalFile.resolve("the-file", theHash, source, fileToKey, fileToHash)
        val input = S3MetaData(theFile, // local exists
          matchByHash = Set.empty, // other no matches
          matchByKey = None) // remote is missing
        it("upload") {
          val expected = List(ToUpload(bucket, theFile)) // upload
          val result = invoke(input)
          assertResult(expected)(result)
        }
      }
      describe("#4 local exists, remote exists, remote no match, other matches - copy") {
        val theHash = MD5Hash("the-hash")
        val theFile = LocalFile.resolve("the-file", theHash, source, fileToKey, fileToHash)
        val theRemoteKey = theFile.remoteKey
        val oldHash = MD5Hash("old-hash")
        val otherRemoteKey = prefix.resolve("other-key")
        val otherRemoteMetadata = RemoteMetaData(otherRemoteKey, theHash, lastModified)
        val oldRemoteMetadata = RemoteMetaData(theRemoteKey,
          hash = oldHash, // remote no match
          lastModified = lastModified)
        val input = S3MetaData(theFile, // local exists
          matchByHash = Set(otherRemoteMetadata), // other matches
          matchByKey = Some(oldRemoteMetadata)) // remote exists
        it("copy from other key") {
          val expected = List(ToCopy(bucket, otherRemoteKey, theHash, theRemoteKey)) // copy
          val result = invoke(input)
          assertResult(expected)(result)
        }
      }
      describe("#5 local exists, remote exists, remote no match, other no matches - upload") {
        val theHash = MD5Hash("the-hash")
        val theFile = LocalFile.resolve("the-file", theHash, source, fileToKey, fileToHash)
        val theRemoteKey = theFile.remoteKey
        val oldHash = MD5Hash("old-hash")
        val theRemoteMetadata = RemoteMetaData(theRemoteKey, oldHash, lastModified)
        val input = S3MetaData(theFile, // local exists
          matchByHash = Set.empty, // remote no match, other no match
          matchByKey = Some(theRemoteMetadata) // remote exists
        )
        it("upload") {
          val expected = List(ToUpload(bucket, theFile)) // upload
          val result = invoke(input)
          assertResult(expected)(result)
        }
      }
      describe("#6 local missing, remote exists - delete") {
        it("TODO") {
          pending
        }
      }
    }
}