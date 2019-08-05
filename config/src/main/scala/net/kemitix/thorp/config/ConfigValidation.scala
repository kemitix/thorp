package net.kemitix.thorp.config

import java.io.File

sealed trait ConfigValidation {

  def errorMessage: String
}

object ConfigValidation {

  case object SourceIsNotADirectory extends ConfigValidation {
    override def errorMessage: String = "Source must be a directory"
  }

  case object SourceIsNotReadable extends ConfigValidation {
    override def errorMessage: String = "Source must be readable"
  }

  case object BucketNameIsMissing extends ConfigValidation {
    override def errorMessage: String = "Bucket name is missing"
  }

  final case class ErrorReadingFile(
      file: File,
      message: String
  ) extends ConfigValidation {
    override def errorMessage: String = s"Error reading file '$file': $message"
  }

}
