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

  private val bufferSize = 8192

  def md5FilePart(file: File, offset: Long, size: Long): String = {
    val fis = new FileInputStream(file)
    fis.skip(offset)
    val md5 = MessageDigest.getInstance("MD5")
    val dis = new DigestInputStream(fis, md5)
    try {
      read(dis, 0, size, new Array[Byte](bufferSize))
    } finally {
      dis.close()
    }
    md5.digest.map("%02x".format(_)).mkString
  }

  def read(dis: DigestInputStream, count: Long, size: Long, buffer: Array[Byte]): Unit =
    if (count + bufferSize > size) dis.read(buffer, 0, (size - count).toInt)
    else {dis.read(buffer);read(dis, count + bufferSize, size, buffer)}

}
