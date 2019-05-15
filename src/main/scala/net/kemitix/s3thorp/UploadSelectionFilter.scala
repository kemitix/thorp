package net.kemitix.s3thorp

import java.io.{File, FileInputStream}
import java.security.{DigestInputStream, MessageDigest}

import net.kemitix.s3thorp.Sync.{LocalFile, MD5Hash}

trait UploadSelectionFilter
  extends Logging {

  private def md5File(localFile: LocalFile): MD5Hash =  {
    val buffer = new Array[Byte](8192)
    val md5 = MessageDigest.getInstance("MD5")

    val dis = new DigestInputStream(new FileInputStream(localFile), md5)
    try { while (dis.read(buffer) != -1) { } } finally { dis.close() }

    md5.digest.map("%02x".format(_)).mkString
  }

  def uploadRequiredFilter(c: Config): Either[File, S3MetaData] => Stream[File] = {
    case Left(file) => {
      logger.info(s"   Created: ${c.relativePath(file)}")
      Stream(file)
    }
    case Right(s3Metadata) => {
      val localHash: MD5Hash = md5File(s3Metadata.localFile)
      if (localHash != s3Metadata.remoteHash) {
        logger.info(s"   Updated: ${c.relativePath(s3Metadata.localFile)}")
        Stream(s3Metadata.localFile)
      }
      else Stream.empty
    }
  }
}
