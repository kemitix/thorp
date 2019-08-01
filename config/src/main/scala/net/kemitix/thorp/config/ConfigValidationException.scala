package net.kemitix.thorp.config

final case class ConfigValidationException(
    errors: Seq[ConfigValidation]
) extends Exception
