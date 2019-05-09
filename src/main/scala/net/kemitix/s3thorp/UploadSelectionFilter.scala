package net.kemitix.s3thorp

import fs2.Stream
import cats.effect.IO
import net.kemitix.s3thorp.Sync.{Hash, LocalFile}
import java.security.{MessageDigest, DigestInputStream}
import java.io.{File, FileInputStream}

trait UploadSelectionFilter {

  private def md5File(localFile: LocalFile): Hash =  {
    val buffer = new Array[Byte](8192)
    val md5 = MessageDigest.getInstance("MD5")

    val dis = new DigestInputStream(new FileInputStream(localFile), md5)
    try { while (dis.read(buffer) != -1) { } } finally { dis.close() }

    md5.digest.map("%02x".format(_)).mkString
  }

  def uploadRequiredFilter: Either[File, S3MetaData] => Stream[IO, File] = {
    case Left(file) => Stream(file)
    case Right(s3Metadata) =>
      Stream.eval(for {
        localHash <- IO(md5File(s3Metadata.localFile))
      } yield (s3Metadata.localFile, localHash)).
        filter { case (_, localHash) => localHash != s3Metadata.remoteHash }.
        map {case (localFile,_) => localFile}
  }

}
