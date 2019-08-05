package net.kemitix.thorp.domain

object Implicits {

  @SuppressWarnings(Array("org.wartremover.warts.Equals"))
  implicit final class AnyOps[A](self: A) {
    def ===(other: A): Boolean = self == other
    def =/=(other: A): Boolean = self != other
  }

}
