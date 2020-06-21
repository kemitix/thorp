package net.kemitix.thorp.config;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.With;
import net.kemitix.thorp.domain.Bucket;
import net.kemitix.thorp.domain.Filter;
import net.kemitix.thorp.domain.RemoteKey;
import net.kemitix.thorp.domain.Sources;

import java.util.Collections;
import java.util.List;

@With
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Configuration {
    public final Bucket bucket;
    public final RemoteKey prefix;
    public final List<Filter> filters;
    public final boolean debug;
    public final boolean batchMode;
    public final int parallel;
    public final Sources sources;
    static Configuration create() {
        return new Configuration(
                Bucket.named(""),
                RemoteKey.create(""),
                Collections.emptyList(),
                false,
                false,
                1,
                Sources.emptySources
        );
    }
}
