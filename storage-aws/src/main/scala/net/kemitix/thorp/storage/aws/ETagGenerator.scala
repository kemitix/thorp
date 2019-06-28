package net.kemitix.thorp.storage.aws

import java.io.File

import cats.implicits._
import cats.effect.IO
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.transfer.TransferManagerConfiguration
import com.amazonaws.services.s3.transfer.internal.TransferManagerUtils
import net.kemitix.thorp.core.MD5HashGenerator
import net.kemitix.thorp.domain.{Logger, MD5Hash}

trait ETagGenerator {

  def eTag(file: File)(implicit l: Logger): IO[String]= {
    val partSize = calculatePartSize(file)
    val parts = numParts(file.length, partSize)
    partsIndex(parts)
      .map(digestChunk(file, partSize)).sequence
      .map(concatenateDigests)
      .map(MD5HashGenerator.hex)
      .map(hash => s"$hash-$parts")
  }

  private def partsIndex(parts: Long) =
    Range.Long(0, parts, 1).toList

  private def concatenateDigests: List[Array[Byte]] => Array[Byte] =
    lab => lab.foldLeft(Array[Byte]())((acc, ab) => acc ++ ab)

  private def calculatePartSize(file: File) = {
    val request = new PutObjectRequest("", "", file)
    val configuration = new TransferManagerConfiguration
    TransferManagerUtils.calculateOptimalPartSize(request, configuration)
  }

  private def numParts(fileLength: Long, optimumPartSize: Long) = {
    val fullParts = Math.floorDiv(fileLength, optimumPartSize)
    val incompletePart = if (Math.floorMod(fileLength, optimumPartSize) > 0) 1 else 0
    fullParts + incompletePart
  }

  def offsets(totalFileSizeBytes: Long, optimalPartSize: Long): List[Long] =
    Range.Long(0, totalFileSizeBytes, optimalPartSize).toList

  def digestChunk(file: File, chunkSize: Long)(chunkNumber: Long)(implicit l: Logger): IO[Array[Byte]] =
    hashChunk(file, chunkNumber, chunkSize).map(_.digest)

  def hashChunk(file: File, chunkNumber: Long, chunkSize: Long)(implicit l: Logger): IO[MD5Hash] =
    MD5HashGenerator.md5FileChunk(file, chunkNumber * chunkSize, chunkSize)
}

object ETagGenerator extends ETagGenerator
