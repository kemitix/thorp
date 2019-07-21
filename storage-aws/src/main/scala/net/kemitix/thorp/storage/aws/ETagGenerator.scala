package net.kemitix.thorp.storage.aws

import java.nio.file.Path

import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.transfer.TransferManagerConfiguration
import com.amazonaws.services.s3.transfer.internal.TransferManagerUtils
import net.kemitix.thorp.core.MD5HashGenerator
import net.kemitix.thorp.domain.MD5Hash
import zio.Task

trait ETagGenerator {

  def eTag(
      path: Path
  ): Task[String] = {
    val partSize = calculatePartSize(path)
    val parts    = numParts(path.toFile.length, partSize)
    Task
      .foreach(partsIndex(parts)) { chunkNumber =>
        digestChunk(path, partSize)(chunkNumber)
      }
      .map(parts => concatenateDigests(parts))
      .map(MD5HashGenerator.hex)
      .map(hash => s"$hash-$parts")
  }

  private def partsIndex(parts: Long) =
    Range.Long(0, parts, 1).toList

  private def concatenateDigests: List[Array[Byte]] => Array[Byte] =
    lab => lab.foldLeft(Array[Byte]())((acc, ab) => acc ++ ab)

  private def calculatePartSize(path: Path) = {
    val request       = new PutObjectRequest("", "", path.toFile)
    val configuration = new TransferManagerConfiguration
    TransferManagerUtils.calculateOptimalPartSize(request, configuration)
  }

  private def numParts(
      fileLength: Long,
      optimumPartSize: Long
  ) = {
    val fullParts = Math.floorDiv(fileLength, optimumPartSize)
    val incompletePart =
      if (Math.floorMod(fileLength, optimumPartSize) > 0) 1
      else 0
    fullParts + incompletePart
  }

  def digestChunk(
      path: Path,
      chunkSize: Long
  )(
      chunkNumber: Long
  ): Task[Array[Byte]] =
    hashChunk(path, chunkNumber, chunkSize).map(_.digest)

  def hashChunk(
      path: Path,
      chunkNumber: Long,
      chunkSize: Long
  ): Task[MD5Hash] =
    MD5HashGenerator.md5FileChunk(path, chunkNumber * chunkSize, chunkSize)

  def offsets(
      totalFileSizeBytes: Long,
      optimalPartSize: Long
  ): List[Long] =
    Range.Long(0, totalFileSizeBytes, optimalPartSize).toList
}

object ETagGenerator extends ETagGenerator
