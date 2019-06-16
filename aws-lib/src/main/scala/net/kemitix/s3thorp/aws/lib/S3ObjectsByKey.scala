package net.kemitix.s3thorp.aws.lib

import com.amazonaws.services.s3.model.S3ObjectSummary
import net.kemitix.thorp.domain.{HashModified, LastModified, MD5Hash, RemoteKey}

object S3ObjectsByKey {

  def byKey(os: Stream[S3ObjectSummary]) =
    os.map { o => {
      val remoteKey = RemoteKey(o.getKey)
      val hash = MD5Hash(o.getETag)
      val lastModified = LastModified(o.getLastModified.toInstant)
      (remoteKey, HashModified(hash, lastModified))
    }}.toMap

}
