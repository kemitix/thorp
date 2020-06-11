package net.kemitix.thorp.domain

import java.io.{File, IOException, PrintWriter}
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, SimpleFileVisitor}

import scala.util.Try

trait TemporaryFolder {

  @SuppressWarnings(Array("org.wartremover.warts.TryPartial"))
  def withDirectory(testCode: Path => Any): Unit = {
    val dir: Path = Files.createTempDirectory("thorp-temp")
    val t         = Try(testCode(dir))
    remove(dir)
    t.get
    ()
  }

  def remove(root: Path): Unit = {
    Files.walkFileTree(
      root,
      new SimpleFileVisitor[Path] {
        override def visitFile(file: Path,
                               attrs: BasicFileAttributes): FileVisitResult = {
          Files.delete(file)
          FileVisitResult.CONTINUE
        }
        override def postVisitDirectory(dir: Path,
                                        exc: IOException): FileVisitResult = {
          Files.delete(dir)
          FileVisitResult.CONTINUE
        }
      }
    )
  }

  def createFile(directory: Path, name: String, contents: String*): File = {
    val _      = directory.toFile.mkdirs
    val file   = directory.resolve(name).toFile
    val writer = new PrintWriter(file, "UTF-8")
    contents.foreach(writer.println)
    writer.close()
    file
  }

  def writeFile(directory: Path, name: String, contents: String*): Unit =
    createFile(directory, name, contents: _*)

}
