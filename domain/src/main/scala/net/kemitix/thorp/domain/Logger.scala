package net.kemitix.thorp.domain

import cats.effect.IO

trait Logger {

  // returns an instance of Logger with debug set as indicated
  // where the current Logger already matches this state, then
  // it returns itself, unmodified
  def withDebug(debug: Boolean): Logger

  def debug(message: => String): IO[Unit]
  def info(message: => String): IO[Unit]
  def warn(message: String): IO[Unit]
  def error(message: String): IO[Unit]

}
