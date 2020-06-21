package net.kemitix.thorp.domain;

import net.kemitix.mon.TypeAlias;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class MD5Hash extends TypeAlias<String> {
    private MD5Hash(String value) {
        super(value);
    }
    public static MD5Hash create(String in) {
        return new MD5Hash(in);
    }
    public static MD5Hash fromDigest(byte[] digest) {
        return new MD5Hash(digestAsString(digest));
    }

    public static String digestAsString(byte[] digest) {
        return IntStream.range(0, digest.length)
                .map(i -> digest[i])
                .mapToObj(b -> String.format("%02x", b))
                .map(s -> s.substring(s.length() - 2, s.length()))
                .flatMap(x -> Stream.of(x.split("")))
                .collect(Collectors.joining());
    }

    public String hash() {
        return QuoteStripper.stripQuotes(String.join("", getValue()));
    }
    public byte[] digest() {
        return HexEncoder.decode(hash());
    }
    public String hash64() {
        return Base64.getEncoder().encodeToString(digest());
    }
}
