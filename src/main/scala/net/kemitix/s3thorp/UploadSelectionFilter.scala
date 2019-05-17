package net.kemitix.s3thorp

import java.io.{File, FileInputStream}
import java.security.{DigestInputStream, MessageDigest}

trait UploadSelectionFilter
  extends Logging {

  private def md5File(localFile: File): MD5Hash =  {
    val buffer = new Array[Byte](8192)
    val md5 = MessageDigest.getInstance("MD5")

    val dis = new DigestInputStream(new FileInputStream(localFile), md5)
    try { while (dis.read(buffer) != -1) { } } finally { dis.close() }

    MD5Hash(md5.digest.map("%02x".format(_)).mkString)
  }

  def uploadRequiredFilter(c: Config): S3MetaData => Stream[ToUpload] = {
    case S3MetaData(localFile, None) => {
      log5(s"   Created: ${c.relativePath(localFile)}")(c)
      Stream(ToUpload(localFile))
    }
    case S3MetaData(localFile, Some((remoteKey, remoteHash, lastModified))) => {
      val localHash: MD5Hash = md5File(localFile)
      if (localHash != remoteHash) {
        log5(s"   Updated: ${c.relativePath(localFile)}")(c)
        Stream(ToUpload(localFile))
      }
      else Stream.empty
    }
  }
}
