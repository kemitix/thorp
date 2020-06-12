package net.kemitix.thorp.domain;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.Arrays;

public class HexEncoder {

    public static String encode(byte[] bytes) {
        return String.format(String.format("%%0%d", bytes.length << 1),
                new BigInteger(1, bytes));
    }
    public static byte[] decode(String hexString) {
        ByteArrayOutputStream bytes =
                new ByteArrayOutputStream(hexString.length() * 4);
        Arrays.stream(hexString
                .replaceAll("[^0-9A-Fa-f]", "")
                .split(""))
                .mapToInt(Integer::parseInt)
                .forEach(bytes::write);
        return bytes.toByteArray();
    }

}
