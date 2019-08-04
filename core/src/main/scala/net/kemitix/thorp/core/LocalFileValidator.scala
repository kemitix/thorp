package net.kemitix.thorp.core

import java.io.File
import java.nio.file.Path

import net.kemitix.thorp.domain.{HashType, LocalFile, MD5Hash, RemoteKey}
import zio.IO

object LocalFileValidator {

  def validate(
      file: File,
      source: File,
      hash: Map[HashType, MD5Hash],
      remoteKey: RemoteKey
  ): IO[Violation, LocalFile] = IO.fromEither {
    for {
      vFile <- validateFile(file)
    } yield LocalFile(vFile, source, hash, remoteKey)
  }

  private def validateFile(file: File) =
    if (file.isDirectory)
      Left(Violation.IsNotAFile(file))
    else
      Right(file)

  sealed trait Violation extends Throwable {
    def getMessage: String
  }
  object Violation {
    case class IsNotAFile(file: File) extends Violation {
      override def getMessage: String = s"Local File must be a file: ${file}"
    }
  }

  def resolve(
      path: String,
      md5Hashes: Map[HashType, MD5Hash],
      source: Path,
      pathToKey: Path => RemoteKey
  ): IO[Violation, LocalFile] = {
    val resolvedPath = source.resolve(path)
    validate(resolvedPath.toFile,
             source.toFile,
             md5Hashes,
             pathToKey(resolvedPath))
  }

}
