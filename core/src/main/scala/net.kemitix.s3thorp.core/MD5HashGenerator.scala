package net.kemitix.s3thorp.core

import java.io.{File, FileInputStream}
import java.security.MessageDigest

import cats.Monad
import cats.implicits._
import net.kemitix.s3thorp.domain.MD5Hash

import scala.collection.immutable.NumericRange

object MD5HashGenerator {

  def md5File[M[_]: Monad](file: File)
                          (implicit info: Int => String => M[Unit]): M[MD5Hash] = {

    val maxBufferSize = 8048

    val defaultBuffer = new Array[Byte](maxBufferSize)

    def openFile = Monad[M].pure(new FileInputStream(file))

    def closeFile = {fis: FileInputStream => Monad[M].pure(fis.close())}

    def nextChunkSize(currentOffset: Long) = {
      // a value between 1 and maxBufferSize
      val toRead = file.length - currentOffset
      val result = Math.min(maxBufferSize, toRead)
      result.toInt
    }

    def readToBuffer(fis: FileInputStream,
                     currentOffset: Long) = {
      val buffer =
        if (nextChunkSize(currentOffset) < maxBufferSize)
          new Array[Byte](nextChunkSize(currentOffset))
        else
          defaultBuffer
      fis read buffer
      buffer
    }

    def digestFile(fis: FileInputStream) =
      Monad[M].pure {
        val md5 = MessageDigest getInstance "MD5"
        NumericRange(0, file.length, maxBufferSize)
          .foreach {
            currentOffset => {
              val buffer = readToBuffer(fis, currentOffset)
              md5 update buffer
            }
          }
        (md5.digest map ("%02x" format _)).mkString
      }

    def readFile: M[String] =
      for {
        fis <- openFile
        md5 <- digestFile(fis)
        _ <- closeFile(fis)
      } yield md5

    for {
      _ <- info(5)(s"md5:reading:size ${file.length}:$file")
      md5 <- readFile
      hash = MD5Hash(md5)
      _ <- info(4)(s"md5:generated:${hash.hash}:$file")
    } yield hash
  }

}
