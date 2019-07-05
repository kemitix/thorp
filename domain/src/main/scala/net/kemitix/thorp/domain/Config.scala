package net.kemitix.thorp.domain

import java.nio.file.Path

final case class Config(bucket: Bucket = Bucket(""),
                        prefix: RemoteKey = RemoteKey(""),
                        filters: List[Filter] = List(),
                        debug: Boolean = false,
                        batchMode: Boolean = false,
                        source: Path)
