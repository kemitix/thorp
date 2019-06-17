package net.kemitix.thorp.domain

import net.kemitix.thorp.domain.QuoteStripper.stripQuotes

final case class MD5Hash(in: String) {

  lazy val hash: String = in filter stripQuotes

}
