package net.kemitix.thorp.domain

import java.io.File

final case class Config(bucket: Bucket = Bucket(""),
                        prefix: RemoteKey = RemoteKey(""),
                        filters: List[Filter] = List(),
                        debug: Boolean = false,
                        source: File)
