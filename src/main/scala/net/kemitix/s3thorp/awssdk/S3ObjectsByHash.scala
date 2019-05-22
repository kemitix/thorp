package net.kemitix.s3thorp.awssdk

import net.kemitix.s3thorp.{KeyModified, LastModified, MD5Hash, RemoteKey}
import software.amazon.awssdk.services.s3.model.S3Object

trait S3ObjectsByHash {

  def byHash(os: Stream[S3Object]): Map[MD5Hash, Set[KeyModified]] = {
    val mD5HashToS3Objects: Map[MD5Hash, Stream[S3Object]] = os.groupBy(o => MD5Hash(o.eTag.filter{c => c != '"'}))
    val hashToModifieds: Map[MD5Hash, Set[KeyModified]] =
      mD5HashToS3Objects.mapValues { os => os.map { o => KeyModified(RemoteKey(o.key), LastModified(o.lastModified())) }.toSet }
    hashToModifieds
  }

}
