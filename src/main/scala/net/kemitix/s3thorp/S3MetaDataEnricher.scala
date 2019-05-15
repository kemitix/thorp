package net.kemitix.s3thorp

import java.io.File

import net.kemitix.s3thorp.Sync.{LastModified, MD5Hash}
import net.kemitix.s3thorp.awssdk.{HashLookup, S3Client}

trait S3MetaDataEnricher
  extends S3Client
    with KeyGenerator
    with Logging {

  def enrichWithS3MetaData(c: Config)(implicit hashLookup: HashLookup): File => Either[File, S3MetaData] = {
    val remoteKey = generateKey(c)_
    file => {
      logger.info(s"- Consider: ${c.relativePath(file)}")
      val key = remoteKey(file)
      objectHead(key).map {
        hlm: (MD5Hash, LastModified) => {
          Right(S3MetaData(file, key, hlm._1.filter { c => c != '"' }, hlm._2))
        }
      }.getOrElse(Left(file))
    }
  }
}
