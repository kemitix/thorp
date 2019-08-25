package net.kemitix.thorp.lib

sealed trait State[S, +A] {
  def apply(s: S): (S, A)

  def map[B](f: A => B): State[S, B] =
    States.state(apply(_) match {
      case (s, a) => (s, f(a))
    })

  def flatMap[B](f: A => State[S, B]): State[S, B] =
    States.state(apply(_) match {
      case (s, a) => f(a)(s)
    })

  def !(s: S): A = apply(s)._2

  def ~>(s: S): S = apply(s)._1

  def withs(f: S => S): State[S, A] = States.state(f andThen apply)
}
object States {
  def state[S, A](f: S => (S, A)): State[S, A] = new State[S, A] {
    def apply(s: S): (S, A) = f(s)
  }

  def init[S]: State[S, S] = state[S, S](s => (s, s))

  def modify[S](f: S => S): State[S, Unit] =
    init[S] flatMap (s => state(_ => (f(s), ())))

  def put[S](s: S): State[S, Unit] = state[S, Unit](_ => (s, ()))

  def gets[S, A](f: S => A): State[S, A] = for (s <- init) yield f(s)
}
