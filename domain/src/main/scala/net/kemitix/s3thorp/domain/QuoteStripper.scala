package net.kemitix.s3thorp.domain

object QuoteStripper {

  def stripQuotes: Char => Boolean = _ != '"'

}
