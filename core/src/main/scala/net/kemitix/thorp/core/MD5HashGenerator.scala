package net.kemitix.thorp.core

import java.io.{File, FileInputStream}
import java.security.MessageDigest

import cats.effect.IO
import net.kemitix.thorp.domain.{Logger, MD5Hash}

import scala.collection.immutable.NumericRange

object MD5HashGenerator {

  def hex(in: Array[Byte]): String = {
    val md5 = MessageDigest getInstance "MD5"
    md5 update in
    (md5.digest map ("%02x" format _)).mkString
  }

  def digest(in: String): Array[Byte] = {
    val md5 = MessageDigest getInstance "MD5"
    md5 update in.getBytes
    md5.digest
  }

  def md5File(file: File)(implicit logger: Logger): IO[MD5Hash] =
    md5FileChunk(file, 0, file.length)

  def md5FileChunk(file: File,
                   offset: Long,
                   size: Long)
                  (implicit logger: Logger): IO[MD5Hash] = {

    val maxBufferSize = 8048
    val defaultBuffer = new Array[Byte](maxBufferSize)
    def openFile = IO {
      val stream = new FileInputStream(file)
      stream skip offset
      stream
    }

    def closeFile = {fis: FileInputStream => IO(fis.close())}

    def nextChunkSize(currentOffset: Long) = {
      // a value between 1 and maxBufferSize
      val toRead = offset + size - currentOffset
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
        NumericRange(offset, offset + size, maxBufferSize)
          .foreach(currentOffset => md5 update readToBuffer(fis, currentOffset))
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
