package net.kemitix.thorp.domain;

import java.util.Arrays;
import java.util.stream.Collectors;

public interface QuoteStripper {
    static String stripQuotes(String in) {
        return Arrays.stream(in.split(""))
                .filter(c -> !c.equals("\""))
                .collect(Collectors.joining());
    }
}
