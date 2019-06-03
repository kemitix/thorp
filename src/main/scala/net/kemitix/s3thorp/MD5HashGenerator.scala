package net.kemitix.s3thorp

import java.io.{File, FileInputStream}
import java.security.{DigestInputStream, MessageDigest}

import net.kemitix.s3thorp.domain.MD5Hash

trait MD5HashGenerator
  extends Logging {

  def md5File(file: File)
             (implicit c: Config): MD5Hash =  {
    val hash = md5FilePart(file, 0, file.length)
    hash
  }

  def md5FilePart(file: File,
                  offset: Long,
                  size: Long)
                 (implicit c: Config): MD5Hash = {
    log5(s"md5:reading:offset $offset:size $size:$file")
    val fis = new FileInputStream(file)
    fis skip offset
    val buffer = new Array[Byte](size.toInt)
    fis read buffer
    val hash = md5PartBody(buffer)
    log5(s"md5:generated:${hash.hash}")
    hash
  }

  def md5PartBody(partBody: Array[Byte]): MD5Hash = {
    val md5 = MessageDigest getInstance "MD5"
    md5 update partBody
    MD5Hash((md5.digest map ("%02x" format _)).mkString)
  }

}
