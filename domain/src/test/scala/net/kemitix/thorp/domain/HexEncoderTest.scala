package net.kemitix.thorp.domain

import java.nio.charset.StandardCharsets

import org.scalatest.FreeSpec

class HexEncoderTest extends FreeSpec {

  val text = "test text to encode to hex"
  val hex  = "74657374207465787420746F20656E636F646520746F20686578"

  "can round trip a hash decode then encode" in {
    val input  = hex
    val result = HexEncoder.encode(HexEncoder.decode(input))
    assertResult(input)(result)
  }
  "can round trip a hash encode then decode" in {
    val input  = hex.getBytes(StandardCharsets.UTF_8)
    val result = HexEncoder.decode(HexEncoder.encode(input))
    assertResult(input)(result)
  }

}
