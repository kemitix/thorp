package net.kemitix.thorp.core

import java.nio.file.Path

import cats.data.{NonEmptyChain, Validated, ValidatedNec}
import cats.implicits._
import net.kemitix.thorp.domain.{Bucket, Config, Sources}

sealed trait ConfigValidator {

  type ValidationResult[A] = ValidatedNec[ConfigValidation, A]

  def validateSourceIsDirectory(source: Path): ValidationResult[Path] =
    if(source.toFile.isDirectory) source.validNec
    else ConfigValidation.SourceIsNotADirectory.invalidNec

  def validateSourceIsReadable(source: Path): ValidationResult[Path] =
    if(source.toFile.canRead) source.validNec
    else ConfigValidation.SourceIsNotReadable.invalidNec

  def validateSource(source: Path): ValidationResult[Path] =
    validateSourceIsDirectory(source)
      .andThen(s =>
        validateSourceIsReadable(s))

  def validateBucket(bucket: Bucket): ValidationResult[Bucket] =
    if (bucket.name.isEmpty) ConfigValidation.BucketNameIsMissing.invalidNec
    else bucket.validNec

  def validateSources(sources: Sources): ValidationResult[Sources] =
    sources.paths
      .map(validateSource).sequence
      .map(_ => sources)

  def validateConfig(config: Config): Validated[NonEmptyChain[ConfigValidation], Config] =
    (
      validateSources(config.sources),
      validateBucket(config.bucket)
    ).mapN((_, _) => config)
}

object ConfigValidator extends ConfigValidator
