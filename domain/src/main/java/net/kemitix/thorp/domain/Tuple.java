package net.kemitix.thorp.domain;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Tuple<A, B> {
    public final A a;
    public final B b;
    public static <A, B> Tuple<A, B> create(A a, B b) {
        return new Tuple<>(a, b);
    }
    public Tuple<B, A> swap() {
        return Tuple.create(b, a);
    }
}
