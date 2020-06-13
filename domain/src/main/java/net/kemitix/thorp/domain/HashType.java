package net.kemitix.thorp.domain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HashType {
    public static HashType MD5 = new HashType();
}
