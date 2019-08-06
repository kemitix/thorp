package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.model.S3ObjectSummary
import net.kemitix.thorp.domain.{MD5Hash, RemoteKey}

object S3ObjectsByKey {

  def byKey(os: Stream[S3ObjectSummary]): Map[RemoteKey, MD5Hash] =
    os.map { o =>
      {
        val remoteKey = RemoteKey(o.getKey)
        val hash      = MD5Hash(o.getETag)
        (remoteKey, hash)
      }
    }.toMap

}
