package net.kemitix.thorp.domain

trait Logger {

  // returns an instance of Logger with debug set as indicated
  // where the current Logger already matches this state, then
  // it returns itself, unmodified
  def withDebug(debug: Boolean): Logger

  def debug(message: => String): Unit
  def info(message: => String): Unit
  def warn(message: String): Unit
  def error(message: String): Unit

}
