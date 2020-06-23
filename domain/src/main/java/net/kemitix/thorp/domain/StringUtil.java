package net.kemitix.thorp.domain;

import java.util.stream.IntStream;

public class StringUtil {
    public static String repeat(String s, int times) {
        StringBuilder sb = new StringBuilder();
        IntStream.range(0, times).forEach(x -> sb.append(s));
        return sb.toString();
    }
}
