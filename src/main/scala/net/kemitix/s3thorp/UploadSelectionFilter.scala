package net.kemitix.s3thorp

import java.io.{File, FileInputStream}
import java.security.{DigestInputStream, MessageDigest}

import net.kemitix.s3thorp.Sync.LocalFile

trait UploadSelectionFilter
  extends Logging {

  private def md5File(localFile: LocalFile): MD5Hash =  {
    val buffer = new Array[Byte](8192)
    val md5 = MessageDigest.getInstance("MD5")

    val dis = new DigestInputStream(new FileInputStream(localFile), md5)
    try { while (dis.read(buffer) != -1) { } } finally { dis.close() }

    MD5Hash(md5.digest.map("%02x".format(_)).mkString)
  }

  def uploadRequiredFilter(c: Config): Either[File, S3MetaData] => Stream[File] = {
    case Left(file) => {
      log5(s"   Created: ${c.relativePath(file)}")(c)
      Stream(file)
    }
    case Right(s3Metadata) => {
      val localHash: MD5Hash = md5File(s3Metadata.localFile)
      if (localHash != s3Metadata.remoteHash) {
        log5(s"   Updated: ${c.relativePath(s3Metadata.localFile)}")(c)
        Stream(s3Metadata.localFile)
      }
      else Stream.empty
    }
  }
}
