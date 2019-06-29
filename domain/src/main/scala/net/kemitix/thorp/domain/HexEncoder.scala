package net.kemitix.thorp.domain

import java.math.BigInteger

trait HexEncoder {

  def encode(bytes: Array[Byte]): String = {
    val bigInteger = new BigInteger(1, bytes)
    String.format("%0" + (bytes.length << 1) + "x", bigInteger)
  }

  def decode(hexString: String): Array[Byte] = {
    val byteArray = new BigInteger(hexString, 16).toByteArray
    if (byteArray(0) == 0) {
      val output = new Array[Byte](byteArray.length - 1)
      System.arraycopy(byteArray, 1, output, 0, output.length)
      output
    } else byteArray
  }

}

object HexEncoder extends HexEncoder
