package net.kemitix.thorp.core

final case class ConfigValidationException(errors: List[ConfigValidation])
    extends Exception {}
