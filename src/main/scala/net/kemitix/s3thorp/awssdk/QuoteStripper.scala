package net.kemitix.s3thorp.awssdk

trait QuoteStripper {

  def stripQuotes: Char => Boolean = _ != '"'

}
