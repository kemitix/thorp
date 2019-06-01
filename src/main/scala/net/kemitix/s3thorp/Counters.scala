package net.kemitix.s3thorp


final case class Counters(uploaded: Int = 0,
                          deleted: Int = 0,
                          copied: Int = 0)
