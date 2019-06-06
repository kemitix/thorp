package net.kemitix.s3thorp.domain

final case class MD5Hash(hash: String) {

  require(!hash.contains("\""))

}
