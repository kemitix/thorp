package net.kemitix.thorp.domain

import java.util.Base64

final case class MD5Hash(in: String)

object MD5Hash {
  def fromDigest(digest: Array[Byte]): MD5Hash =
    MD5Hash((digest map ("%02x" format _)).mkString)
  def hash(md5Hash: MD5Hash): String = QuoteStripper.stripQuotes(md5Hash.in)
  def digest(md5Hash: MD5Hash): Array[Byte] =
    HexEncoder.decode(MD5Hash.hash(md5Hash))
  def hash64(md5Hash: MD5Hash): String =
    Base64.getEncoder.encodeToString(MD5Hash.digest(md5Hash))
}
