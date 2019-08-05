package net.kemitix.thorp.domain

import java.util.Base64

import net.kemitix.thorp.domain.QuoteStripper.stripQuotes

final case class MD5Hash(in: String)

object MD5Hash {
  def fromDigest(digest: Array[Byte]): MD5Hash =
    MD5Hash((digest map ("%02x" format _)).mkString)
  def hash(md5Hash: MD5Hash): String = md5Hash.in.filter(stripQuotes)
  def digest(md5Hash: MD5Hash): Array[Byte] =
    HexEncoder.decode(MD5Hash.hash(md5Hash))
  def hash64(md5Hash: MD5Hash): String =
    Base64.getEncoder.encodeToString(MD5Hash.digest(md5Hash))
}
