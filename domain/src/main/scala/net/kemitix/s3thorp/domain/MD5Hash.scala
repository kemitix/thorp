package net.kemitix.s3thorp.domain

import net.kemitix.s3thorp.domain.QuoteStripper.stripQuotes

final case class MD5Hash(in: String) {

  lazy val hash: String = in filter stripQuotes

}
