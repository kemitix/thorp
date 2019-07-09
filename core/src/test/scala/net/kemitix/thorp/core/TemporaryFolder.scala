package net.kemitix.thorp.core

import java.io.IOException
import java.nio.file.{Files, Path}

trait TemporaryFolder {

  def withDirectory(testCode: Path => Any): Unit = {
    val dir: Path = Files.createTempDirectory("thorp-temp")
    try {
      testCode(dir)
    }
    finally {
      remove(dir)
    }
  }

  import java.nio.file.attribute.BasicFileAttributes
  import java.nio.file.{FileVisitResult, Files, Path, SimpleFileVisitor}

  def remove(root: Path): Unit = {
    Files.walkFileTree(root, new SimpleFileVisitor[Path] {
      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        Files.delete(file)
        FileVisitResult.CONTINUE
      }
      override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
        Files.delete(dir)
        FileVisitResult.CONTINUE
      }
    })
  }
}