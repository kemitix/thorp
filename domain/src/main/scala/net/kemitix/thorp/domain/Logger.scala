package net.kemitix.thorp.domain

trait Logger[M[_]] {

  // returns an instance of Logger with debug set as indicated
  // where the current Logger already matches this state, then
  // it returns itself, unmodified
  def withDebug(debug: Boolean): Logger[M]

  def debug(message: => String): M[Unit]
  def info(message: => String): M[Unit]
  def warn(message: String): M[Unit]
  def error(message: String): M[Unit]

}
