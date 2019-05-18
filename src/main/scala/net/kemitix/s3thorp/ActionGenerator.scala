package net.kemitix.s3thorp

trait ActionGenerator
  extends MD5HashGenerator
  with Logging {

  def createActions(s3MetaData: S3MetaData)
                   (implicit c: Config): Stream[ToUpload] =
    s3MetaData match {
      case S3MetaData(localFile, None) => {
        log5(s"   Created: ${c.relativePath(localFile)}")(c)
        Stream(ToUpload(localFile))
      }
      case S3MetaData(localFile, Some((remoteKey, remoteHash, lastModified))) =>
        if (md5File(localFile) != remoteHash) {
          log5(s"   Updated: ${c.relativePath(localFile)}")(c)
          Stream(ToUpload(localFile))
        }
        else Stream.empty
    }
}
