package net.kemitix.s3thorp.core

object QuoteStripper {

  def stripQuotes: Char => Boolean = _ != '"'

}
