package net.kemitix.s3thorp

import java.io.File

import fs2.Stream

import cats.effect.IO

trait LocalFileStream {

  def streamDirectoryPaths(file: File): Stream[IO, File] =
  {
    Stream.eval(IO(file)).
      flatMap(file => Stream.fromIterator[IO, File](dirPaths(file))).
      flatMap(recurseIntoSubDirectories)
  }

  private def dirPaths(file: File): Iterator[File] = {
    Option(file.listFiles).map(_.iterator).
      getOrElse(throw new IllegalArgumentException(s"Directory not found $file"))
  }

  private def recurseIntoSubDirectories: File => Stream[IO, File] =
    file =>
      if (file.isDirectory) streamDirectoryPaths(file)
      else Stream(file)


}
