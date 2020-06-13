package net.kemitix.thorp.config

final case class SimpleLens[A, B](field: A => B, update: A => B => A) {

  def composeLens[C](other: SimpleLens[B, C]): SimpleLens[A, C] =
    SimpleLens[A, C](
      a => other.field(field(a)),
      a => c => update(a)(other.update(field(a))(c))
    )

  def ^|->[C](other: SimpleLens[B, C]): SimpleLens[A, C] = composeLens(other)

  def set(b: B)(a: A): A = update(a)(b)

  def get(a: A): B = field(a)

  def modify(f: B => B)(a: A): A = update(a)(f(field(a)))
}
