package net.kemitix.thorp.storage.aws.hasher

import java.nio.file.Path

import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.transfer.TransferManagerConfiguration
import com.amazonaws.services.s3.transfer.internal.TransferManagerUtils
import net.kemitix.thorp.domain.HashType.MD5
import net.kemitix.thorp.filesystem.{FileSystem, Hasher}
import zio.{RIO, ZIO}

private trait ETagGenerator {

  def eTag(path: Path): RIO[Hasher with FileSystem, String]

  def offsets(totalFileSizeBytes: Long, optimalPartSize: Long): List[Long]

}

private object ETagGenerator extends ETagGenerator {

  override def eTag(path: Path): RIO[Hasher with FileSystem, String] = {
    val partSize = calculatePartSize(path)
    val parts    = numParts(path.toFile.length, partSize)
    eTagHex(path, partSize, parts)
      .map(hash => s"$hash-$parts")
  }

  override def offsets(totalFileSizeBytes: Long,
                       optimalPartSize: Long): List[Long] =
    Range.Long(0, totalFileSizeBytes, optimalPartSize).toList

  private def eTagHex(path: Path, partSize: Long, parts: Long) =
    ZIO
      .foreach(partsIndex(parts))(digestChunk(path, partSize))
      .map(concatenateDigests) >>= Hasher.hex

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

  private def digestChunk(
      path: Path,
      chunkSize: Long
  )(chunkNumber: Long) =
    Hasher
      .hashObjectChunk(path, chunkNumber, chunkSize)
      .map(_(MD5))
      .map(x => x.digest)
}
