package net.kemitix.thorp.domain

import java.io.File

final case class Config(
                         bucket: Bucket = Bucket(""),
                         prefix: RemoteKey = RemoteKey(""),
                         filters: List[Filter] = List(),
                         multiPartThreshold: Long = 1024 * 1024 * 5,
                         maxRetries: Int = 3,
                         debug: Boolean = false,
                         source: File
) {
  require(multiPartThreshold >= 1024 * 1024 * 5, s"Threshold for multi-part upload is 5Mb: '$multiPartThreshold'")
}
