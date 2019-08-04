package net.kemitix.thorp.domain

import java.io.{File, IOException, PrintWriter}
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, SimpleFileVisitor}

import scala.util.Try

trait TemporaryFolder {

  def withDirectory(testCode: Path => Any): Any = {
    val dir: Path = Files.createTempDirectory("thorp-temp")
    val t         = Try(testCode(dir))
    remove(dir)
    t.get
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

  def createFile(path: Path, name: String, content: String*): File = {
    writeFile(path, name, content: _*)
    path.resolve(name).toFile
  }

  def writeFile(directory: Path, name: String, contents: String*): File = {
    directory.toFile.mkdirs
    val file   = directory.resolve(name).toFile
    val writer = new PrintWriter(file, "UTF-8")
    contents.foreach(writer.println)
    writer.close()
    file
  }

}
