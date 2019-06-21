package net.kemitix.thorp.core

import java.io.{File, FileInputStream}
import java.security.MessageDigest

import cats.effect.IO
import net.kemitix.thorp.domain.{Logger, MD5Hash}

import scala.collection.immutable.NumericRange

object MD5HashGenerator {

  def md5File(file: File)
             (implicit logger: Logger): IO[MD5Hash] = {

    val maxBufferSize = 8048
    val defaultBuffer = new Array[Byte](maxBufferSize)
    def openFile = IO.pure(new FileInputStream(file))
    def closeFile = {fis: FileInputStream => IO(fis.close())}

    def nextChunkSize(currentOffset: Long) = {
      // a value between 1 and maxBufferSize
      val toRead = file.length - currentOffset
      val result = Math.min(maxBufferSize, toRead)
      result.toInt
    }

    def readToBuffer(fis: FileInputStream,
                     currentOffset: Long) = {
      val buffer =
        if (nextChunkSize(currentOffset) < maxBufferSize) new Array[Byte](nextChunkSize(currentOffset))
        else defaultBuffer
      fis read buffer
      buffer
    }

    def digestFile(fis: FileInputStream) =
      IO {
        val md5 = MessageDigest getInstance "MD5"
        NumericRange(0, file.length, maxBufferSize)
          .foreach { currentOffset => {
              val buffer = readToBuffer(fis, currentOffset)
              md5 update buffer
            }}
        md5.digest
      }

    def readFile =
      for {
        fis <- openFile
        digest <- digestFile(fis)
        _ <- closeFile(fis)
      } yield digest

    for {
      _ <- logger.debug(s"md5:reading:size ${file.length}:$file")
      digest <- readFile
      hash = MD5Hash.fromDigest(digest)
      _ <- logger.debug(s"md5:generated:${hash.hash}:$file")
    } yield hash
  }

}
