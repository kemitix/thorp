package net.kemitix.s3thorp.core

import java.io.{File, FileInputStream}
import java.security.MessageDigest

import cats.effect.IO
import net.kemitix.s3thorp.domain.MD5Hash

object MD5HashGenerator {

  def md5File(file: File)
             (implicit info: Int => String => Unit): IO[MD5Hash] =
    md5FilePart(file, 0, file.length)

  def md5FilePart(file: File,
                  offset: Long,
                  size: Long)
                 (implicit info: Int => String => Unit): IO[MD5Hash] = {
    val buffer = new Array[Byte](size.toInt)

    def readIntoBuffer = {
      fis: FileInputStream =>
        IO {
          fis skip offset
          fis read buffer
          fis
        }
    }

    def closeFile = {fis: FileInputStream => IO(fis.close())}

    def openFile = IO(new FileInputStream(file))

    def readFile = openFile.bracket(readIntoBuffer)(closeFile)

    for {
      _ <- IO(info(5)(s"md5:reading:offset $offset:size $size:$file"))
      _ <- readFile
      hash = md5PartBody(buffer)
      _ <- IO (info(5)(s"md5:generated:${hash.hash}"))
    } yield hash
  }

  def md5PartBody(partBody: Array[Byte]): MD5Hash = {
    val md5 = MessageDigest getInstance "MD5"
    md5 update partBody
    MD5Hash((md5.digest map ("%02x" format _)).mkString)
  }

}
