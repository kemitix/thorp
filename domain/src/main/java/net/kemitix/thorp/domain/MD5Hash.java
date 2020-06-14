package net.kemitix.thorp.domain;

import net.kemitix.mon.TypeAlias;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class MD5Hash extends TypeAlias<List<String>> {
    private MD5Hash(List<String> value) {
        super(value);
    }
    public static MD5Hash create(String in) {
        return new MD5Hash(Arrays.asList(in.split("")));
    }
    public static MD5Hash fromDigest(Byte[] digest) {
        return new MD5Hash(
                Arrays.stream(digest)
                        .map(b -> String.format("%02x", b))
                        .collect(Collectors.toList()));
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
