package net.kemitix.thorp.domain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class HashType {
    public final String label;
    public static HashType MD5 = new HashType("MD5");
    public static HashType DUMMY = new HashType("Dummy"); // testing only
}
