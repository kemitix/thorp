package net.kemitix.thorp.domain;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class HexEncoder {

    public static String encode(byte[] bytes) {
        return String.format("%0" + (bytes.length << 1) + "x",
                new BigInteger(1, bytes))
                .toUpperCase();
    }
    public static byte[] decode(String hexString) {
        ByteArrayOutputStream bytes =
                new ByteArrayOutputStream(hexString.length() * 4);
        List<String> hexBytes = Arrays.stream(hexString
                .replaceAll("[^0-9A-Fa-f]", "")
                .split("")).collect(Collectors.toList());
        sliding(hexBytes, 2)
                .map(hb -> String.join("", hb))
                .mapToInt(hex -> Integer.parseInt(hex, 16))
                .forEach(bytes::write);
        return bytes.toByteArray();
    }

    public static <T> Stream<List<T>> sliding(List<T> list, int size) {
        if(size > list.size())
            return Stream.empty();
        return IntStream.range(0, list.size()-size+1)
                .filter(i -> i % size == 0)
                .mapToObj(start -> list.subList(start, start+size));
    }
}
