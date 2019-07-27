package net.kemitix.thorp.config

final case class ConfigValidationException(
    errors: List[ConfigValidation]
) extends Exception
