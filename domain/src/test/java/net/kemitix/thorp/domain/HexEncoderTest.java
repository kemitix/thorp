package net.kemitix.thorp.domain;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

public class HexEncoderTest
        implements WithAssertions {

    private String text = "test text to encode to hex";
    private String hex  = "74657374207465787420746F20656E636F646520746F20686578";

    @Test
    @DisplayName("can round trip a hash decode then encode")
    public void roundTripDecodeEncode() {
        String result = HexEncoder.encode(HexEncoder.decode(hex));
        assertThat(result).isEqualTo(hex);
    }

    @Test
    @DisplayName("can round trip a hash encode then decode")
    public void roundTripEncodeDecode() {
        byte[] input  = hex.getBytes(StandardCharsets.UTF_8);
        byte[] result = HexEncoder.decode(HexEncoder.encode(input));
        assertThat(result).isEqualTo(input);
    }
}
