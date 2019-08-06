package net.kemitix.thorp.domain

import Implicits._

object QuoteStripper {

  def stripQuotes: Char => Boolean = _ =/= '"'

}
