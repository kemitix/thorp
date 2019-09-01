package net.kemitix.thorp.domain

object NonUnit {
  @specialized def ~*[A](evaluateForSideEffectOnly: A): Unit = {
    val _ = evaluateForSideEffectOnly
    () //Return unit to prevent warning due to discarding value
  }
}
