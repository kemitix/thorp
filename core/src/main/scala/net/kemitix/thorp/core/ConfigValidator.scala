package net.kemitix.thorp.core

import java.io.File

import cats.data.{NonEmptyChain, Validated, ValidatedNec}
import cats.implicits._
import net.kemitix.thorp.domain.{Bucket, Config}

sealed trait ConfigValidator {

  type ValidationResult[A] = ValidatedNec[ConfigValidation, A]

  def validateSourceIsDirectory(source: File): ValidationResult[File] =
    if(source.isDirectory) source.validNec
    else ConfigValidation.SourceIsNotADirectory.invalidNec

  def validateSourceIsReadable(source: File): ValidationResult[File] =
    if(source.canRead) source.validNec
    else ConfigValidation.SourceIsNotReadable.invalidNec

  def validateSource(source: File): ValidationResult[File] =
    validateSourceIsDirectory(source).andThen(s => validateSourceIsReadable(s))

  def validateBucket(bucket: Bucket): ValidationResult[Bucket] =
    if (bucket.name.isEmpty) ConfigValidation.BucketNameIsMissing.invalidNec
    else bucket.validNec

  def validateConfig(config: Config): Validated[NonEmptyChain[ConfigValidation], Config] =
    (
      validateSource(config.source),
      validateBucket(config.bucket)
    ).mapN((_, _) => config)
}

object ConfigValidator extends ConfigValidator
