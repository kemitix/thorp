package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.model.S3ObjectSummary
import net.kemitix.thorp.domain.{MD5Hash, RemoteKey}

import scala.collection.MapView

object S3ObjectsByKey {

  def byKey(os: LazyList[S3ObjectSummary]): MapView[RemoteKey, MD5Hash] =
    os.map { o =>
        {
          val remoteKey = RemoteKey.create(o.getKey)
          val hash      = MD5Hash.create(o.getETag)
          (remoteKey, hash)
        }
      }
      .toMap
      .view

}
