package net.kemitix.thorp.core

import java.io.File
import java.nio.file.Path

import net.kemitix.thorp.domain.{
  HashType,
  LocalFile,
  MD5Hash,
  RemoteKey,
  Sources
}
import zio.{IO, ZIO}

object LocalFileValidator {

  def validate(
      path: Path,
      source: File,
      hash: Map[HashType, MD5Hash],
      sources: Sources,
      prefix: RemoteKey
  ): IO[Violation, LocalFile] =
    for {
      file      <- validateFile(path.toFile)
      remoteKey <- validateRemoteKey(sources, prefix, path)
    } yield LocalFile(file, source, hash, remoteKey)

  private def validateFile(file: File): IO[Violation, File] =
    if (file.isDirectory)
      ZIO.fail(Violation.IsNotAFile(file))
    else
      ZIO.succeed(file)

  private def validateRemoteKey(sources: Sources,
                                prefix: RemoteKey,
                                path: Path): IO[Violation, RemoteKey] =
    KeyGenerator
      .generateKey(sources, prefix)(path)
      .mapError(e => Violation.InvalidRemoteKey(path, e))

  sealed trait Violation extends Throwable {
    def getMessage: String
  }
  object Violation {
    final case class IsNotAFile(file: File) extends Violation {
      override def getMessage: String = s"Local File must be a file: ${file}"
    }
    final case class InvalidRemoteKey(path: Path, e: Throwable)
        extends Violation {
      override def getMessage: String =
        s"Remote Key for '${path}' is invalid: ${e.getMessage}"
    }
  }

  def resolve(
      path: String,
      md5Hashes: Map[HashType, MD5Hash],
      source: Path,
      sources: Sources,
      prefix: RemoteKey
  ): IO[Violation, LocalFile] = {
    val resolvedPath = source.resolve(path)
    validate(resolvedPath, source.toFile, md5Hashes, sources, prefix)
  }

}
