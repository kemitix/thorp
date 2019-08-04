package net.kemitix.thorp.domain

import java.util.Base64

import net.kemitix.thorp.domain.QuoteStripper.stripQuotes

final case class MD5Hash(
    in: String
) {

  lazy val digest: Array[Byte] = HexEncoder.decode(MD5Hash.hash(this))

  lazy val hash64: String = Base64.getEncoder.encodeToString(digest)
}

object MD5Hash {
  def fromDigest(digest: Array[Byte]): MD5Hash = {
    val hexDigest = (digest map ("%02x" format _)).mkString
    MD5Hash(hexDigest)
  }
  def hash(md5Hash: MD5Hash): String = md5Hash.in filter stripQuotes
}
