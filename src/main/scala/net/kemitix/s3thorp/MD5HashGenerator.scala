package net.kemitix.s3thorp

import java.io.{File, FileInputStream}
import java.security.{DigestInputStream, MessageDigest}

trait MD5HashGenerator {

  def md5File(localFile: File): MD5Hash =  {
    val buffer = new Array[Byte](8192)
    val md5 = MessageDigest.getInstance("MD5")

    val dis = new DigestInputStream(new FileInputStream(localFile), md5)
    try { while (dis.read(buffer) != -1) { } } finally { dis.close() }

    MD5Hash(md5.digest.map("%02x".format(_)).mkString)
  }

}
