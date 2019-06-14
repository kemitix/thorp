package net.kemitix.s3thorp.core

trait Logger[M[_]] {

  def debug(message: => String): M[Unit]
  def info(message: => String): M[Unit]
  def warn(message: String): M[Unit]
  def error(message: String): M[Unit]

}
