package net.kemitix.thorp.filesystem

import scala.jdk.CollectionConverters._
import net.kemitix.thorp.domain.{RemoteKey, Sources}
import org.scalatest.FreeSpec
import zio.DefaultRuntime

class FileSystemTest extends FreeSpec with TemporaryFolder {

  "Live" - {
    "hasLocalFile" - {
      "file exists" in {
        withDirectory(dir => {
          val filename = "filename"
          createFile(dir, filename, contents = "")
          val remoteKey = RemoteKey.create(filename)
          val sources   = Sources.create(List(dir).asJava)
          val prefix    = RemoteKey.create("")
          val program   = FileSystem.hasLocalFile(sources, prefix, remoteKey)
          val result = new DefaultRuntime {}
            .unsafeRunSync(program.provide(FileSystem.Live))
            .toEither
          val expected = true
          assertResult(Right(expected))(result)
        })
      }
      "file does not exist" in {
        withDirectory(dir => {
          val filename  = "filename"
          val remoteKey = RemoteKey.create(filename)
          val sources   = Sources.create(List(dir).asJava)
          val prefix    = RemoteKey.create("")
          val program   = FileSystem.hasLocalFile(sources, prefix, remoteKey)
          val result = new DefaultRuntime {}
            .unsafeRunSync(program.provide(FileSystem.Live))
            .toEither
          val expected = false
          assertResult(Right(expected))(result)
        })
      }
    }
  }
}
