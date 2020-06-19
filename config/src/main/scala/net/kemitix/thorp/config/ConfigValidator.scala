package net.kemitix.thorp.config

import java.nio.file.Path

import net.kemitix.thorp.domain.{Bucket, Sources}
import zio.Task

import scala.jdk.CollectionConverters._

sealed trait ConfigValidator {

  def validateConfig(
      config: Configuration
  ): Task[Configuration] =
    Task((for {
      _ <- validateSources(config.sources)
      _ <- validateBucket(config.bucket)
    } yield config).toOption.get)

  def validateBucket(bucket: Bucket): Either[List[ConfigValidation], Bucket] =
    if (bucket.name.isEmpty) Left(List(ConfigValidation.bucketNameIsMissing))
    else Right(bucket)

  def validateSources(
      sources: Sources): Either[List[ConfigValidation], Sources] =
    sources.paths.asScala.foldLeft(List[ConfigValidation]()) {
      (acc: List[ConfigValidation], path) =>
        {
          validateSource(path) match {
            case Left(errors) => acc ++ errors
            case Right(_)     => acc
          }
        }
    } match {
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
    else Left(List(ConfigValidation.sourceIsNotADirectory))

  def validateSourceIsReadable(
      source: Path): Either[List[ConfigValidation], Path] =
    if (source.toFile.canRead) Right(source)
    else Left(List(ConfigValidation.sourceIsNotReadable))

}

object ConfigValidator extends ConfigValidator
