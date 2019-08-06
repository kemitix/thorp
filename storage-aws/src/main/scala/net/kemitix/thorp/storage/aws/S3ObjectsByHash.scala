package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.model.S3ObjectSummary
import net.kemitix.thorp.domain.{MD5Hash, RemoteKey}

object S3ObjectsByHash {

  def byHash(
      os: Stream[S3ObjectSummary]
  ): Map[MD5Hash, Set[RemoteKey]] = {
    val mD5HashToS3Objects: Map[MD5Hash, Stream[S3ObjectSummary]] =
      os.groupBy(o => MD5Hash(o.getETag.filter(_ != '"')))
    mD5HashToS3Objects.mapValues { os =>
      os.map(_.getKey).map(RemoteKey(_)).toSet
    }
  }

}
