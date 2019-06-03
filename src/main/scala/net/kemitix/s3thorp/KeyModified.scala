package net.kemitix.s3thorp

import net.kemitix.s3thorp.domain.{LastModified, RemoteKey}

final case class KeyModified(key: RemoteKey,
                             modified: LastModified) {

}
