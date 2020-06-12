package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.model.S3ObjectSummary
import net.kemitix.thorp.domain.{MD5Hash, RemoteKey}

import scala.collection.MapView

object S3ObjectsByHash {

  def byHash(
      os: LazyList[S3ObjectSummary]
  ): MapView[MD5Hash, RemoteKey] =
    os.map { o =>
        MD5Hash.create(o.getETag) -> RemoteKey(o.getKey)
      }
      .toMap
      .view

}
