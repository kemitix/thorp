package net.kemitix.thorp.storage.aws;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class HashType extends net.kemitix.thorp.domain.HashType {
    public static net.kemitix.thorp.domain.HashType ETag = new HashType();
}
