package net.kemitix.s3thorp.aws.lib

import com.amazonaws.services.s3.model.S3ObjectSummary
import net.kemitix.s3thorp.domain.{HashModified, LastModified, MD5Hash, RemoteKey}
import net.kemitix.s3thorp.core.QuoteStripper.stripQuotes

object S3ObjectsByKey {

  def byKey(os: Stream[S3ObjectSummary]) =
    os.map { o => {
      val remoteKey = RemoteKey(o.getKey)
      val hash = MD5Hash(o.getETag filter stripQuotes)
      val lastModified = LastModified(o.getLastModified.toInstant)
      (remoteKey, HashModified(hash, lastModified))
    }}.toMap

}
