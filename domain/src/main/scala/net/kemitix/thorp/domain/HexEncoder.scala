package net.kemitix.thorp.domain

import java.math.BigInteger

trait HexEncoder {

  def encode(bytes: Array[Byte]): String =
    String.format("%0" + (bytes.length << 1) + "x", new BigInteger(1, bytes))

  def decode(hexString: String): Array[Byte] =
    hexString
      .replaceAll("[^0-9A-Fa-f]", "")
      .sliding(2, 2)
      .toArray
      .map(Integer.parseInt(_, 16).toByte)

}

object HexEncoder extends HexEncoder
