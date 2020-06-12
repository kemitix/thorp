package net.kemitix.thorp.domain;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.With;

@With
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Counters {
    public final int uploaded;
    public final int deleted;
    public final int copied;
    public final int errors;
    public static Counters empty() {
        return new Counters(0, 0, 0, 0);
    }
    public Counters incrementUploaded() {
        return withUploaded(uploaded + 1);
    }
    public Counters incrementDeleted() {
        return withDeleted(deleted + 1);
    }
    public Counters incrementCopied() {
        return withCopied(copied + 1);
    }
    public Counters incrementErrors() {
        return withErrors(errors + 1);
    }
}
