package net.kemitix.s3thorp.aws.lib

trait QuoteStripper {

  def stripQuotes: Char => Boolean = _ != '"'

}
