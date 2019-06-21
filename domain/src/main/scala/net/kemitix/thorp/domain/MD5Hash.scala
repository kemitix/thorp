package net.kemitix.thorp.domain

import java.util.Base64

import net.kemitix.thorp.domain.QuoteStripper.stripQuotes

final case class MD5Hash(in: String, hash64: Option[String] = None) {

  lazy val hash: String = in filter stripQuotes

}

object MD5Hash {
  def fromDigest(digest: Array[Byte]): MD5Hash =
    MD5Hash((digest map ("%02x" format _)).mkString, Some(Base64.getEncoder.encodeToString(digest)))
}
