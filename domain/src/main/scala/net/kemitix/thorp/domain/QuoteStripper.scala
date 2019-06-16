package net.kemitix.thorp.domain

object QuoteStripper {

  def stripQuotes: Char => Boolean = _ != '"'

}
