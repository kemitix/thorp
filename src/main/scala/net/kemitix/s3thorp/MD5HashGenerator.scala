package net.kemitix.s3thorp

import java.io.{File, FileInputStream}
import java.security.{DigestInputStream, MessageDigest}

trait MD5HashGenerator
  extends Logging {

  def md5File(file: File)(implicit c: Config): MD5Hash =  {
    log5(s"md5file:reading:${file.length}:$file")
    val buffer = new Array[Byte](8192)
    val md5 = MessageDigest.getInstance("MD5")
    val dis = new DigestInputStream(new FileInputStream(file), md5)
    try { while (dis.read(buffer) != -1) { } } finally { dis.close() }
    val hash = md5.digest.map("%02x".format(_)).mkString
    log5(s"md5file:generated:$hash:$file")
    MD5Hash(hash)
  }

}
