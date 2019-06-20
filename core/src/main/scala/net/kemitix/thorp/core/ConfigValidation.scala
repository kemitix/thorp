package net.kemitix.thorp.core

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

}
