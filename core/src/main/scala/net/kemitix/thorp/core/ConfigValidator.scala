package net.kemitix.thorp.core

import java.nio.file.Path

import net.kemitix.thorp.domain.{Bucket, Config, Sources}
import zio.IO

sealed trait ConfigValidator {

  def validateConfig(
      config: Config
  ): IO[List[ConfigValidation], Config] = IO.fromEither {
    for {
      _ <- validateSources(config.sources)
      _ <- validateBucket(config.bucket)
    } yield config
  }

  def validateBucket(bucket: Bucket): Either[List[ConfigValidation], Bucket] =
    if (bucket.name.isEmpty) Left(List(ConfigValidation.BucketNameIsMissing))
    else Right(bucket)

  def validateSources(
      sources: Sources): Either[List[ConfigValidation], Sources] =
    (for {
      x <- sources.paths.foldLeft(List[ConfigValidation]()) {
        (acc: List[ConfigValidation], path) =>
          {
            validateSource(path) match {
              case Left(errors) => acc ++ errors
              case Right(_)     => acc
            }
          }
      }
    } yield x) match {
      case Nil    => Right(sources)
      case errors => Left(errors)
    }

  def validateSource(source: Path): Either[List[ConfigValidation], Path] =
    for {
      _ <- validateSourceIsDirectory(source)
      _ <- validateSourceIsReadable(source)
    } yield source

  def validateSourceIsDirectory(
      source: Path): Either[List[ConfigValidation], Path] =
    if (source.toFile.isDirectory) Right(source)
    else Left(List(ConfigValidation.SourceIsNotADirectory))

  def validateSourceIsReadable(
      source: Path): Either[List[ConfigValidation], Path] =
    if (source.toFile.canRead) Right(source)
    else Left(List(ConfigValidation.SourceIsNotReadable))

}

object ConfigValidator extends ConfigValidator
