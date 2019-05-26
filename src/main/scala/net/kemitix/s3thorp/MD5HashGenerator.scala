package net.kemitix.s3thorp

import java.io.{File, FileInputStream}
import java.security.{DigestInputStream, MessageDigest}

trait MD5HashGenerator
  extends Logging {

  def md5File(file: File)(implicit c: Config): MD5Hash =  {
    log5(s"md5file:reading:${file.length}:$file")
    val hash = md5FilePart(file, 0, file.length)
    log5(s"md5file:generated:$hash:$file")
    MD5Hash(hash)
  }

  def md5FilePart(file: File, offset: Long, size: Long): String = {
    val fis = new FileInputStream(file)
    fis skip offset
    val buffer = new Array[Byte](size.toInt)
    fis read buffer
    md5PartBody(buffer)
  }

  def md5PartBody(partBody: Array[Byte]): String = {
    val md5 = MessageDigest getInstance "MD5"
    md5 update partBody
    (md5.digest map ("%02x" format _)).mkString
  }

}
