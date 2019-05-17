package net.kemitix.s3thorp

import java.io.File

import net.kemitix.s3thorp.awssdk.{HashLookup, S3Client}

trait S3MetaDataEnricher
  extends S3Client
    with KeyGenerator
    with Logging {

  def enrichWithS3MetaData(c: Config)(implicit hashLookup: HashLookup): File => S3MetaData = {
    val remoteKey = generateKey(c)_
    file => {
      log3(s"- Consider: ${c.relativePath(file)}")(c)
      val key = remoteKey(file)
      objectHead(key).map {
        hlm: (MD5Hash, LastModified) => {
            S3MetaData(
              localFile = file,
              remote = Some((
                key,
                MD5Hash(hlm._1.hash.filter { c => c != '"' }),
                hlm._2)))
        }
      }.getOrElse(S3MetaData(localFile = file, remote = None))
    }
  }
}
