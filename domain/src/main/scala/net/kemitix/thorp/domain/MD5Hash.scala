package net.kemitix.thorp.domain

import java.util.Base64

import net.kemitix.thorp.domain.QuoteStripper.stripQuotes

final case class MD5Hash(in: String) {

  lazy val hash: String = in filter stripQuotes

  lazy val digest: Array[Byte] = HexEncoder.decode(hash)

  lazy val hash64: String = Base64.getEncoder.encodeToString(digest)
}

object MD5Hash {
  def fromDigest(digest: Array[Byte]): MD5Hash = {
    val hexDigest = (digest map ("%02x" format _)).mkString
    MD5Hash(hexDigest)
  }
}
