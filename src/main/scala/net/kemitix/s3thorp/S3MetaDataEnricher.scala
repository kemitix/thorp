package net.kemitix.s3thorp

import java.io.File

import net.kemitix.s3thorp.awssdk.{HashLookup, S3Client}

trait S3MetaDataEnricher
  extends S3Client
    with KeyGenerator
    with Logging {

  def enrichWithS3MetaData(c: Config)(implicit hashLookup: HashLookup): File => Either[File, S3MetaData] = {
    val remoteKey = generateKey(c)_
    file => {
      log3(s"- Consider: ${c.relativePath(file)}")(c)
      val key = remoteKey(file)
      objectHead(key).map {
        hlm: (MD5Hash, LastModified) => {
          Right(
            S3MetaData(
              localFile = file,
              remotePath = key,
              remoteHash = MD5Hash(hlm._1.hash.filter { c => c != '"' }),
              remoteLastModified = hlm._2))
        }
      }.getOrElse(Left(file))
    }
  }
}
