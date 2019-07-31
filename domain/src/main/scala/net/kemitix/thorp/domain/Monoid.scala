package net.kemitix.thorp.domain

trait Monoid[T] {
  def zero: T
  def op(t1: T, t2: T): T
}
