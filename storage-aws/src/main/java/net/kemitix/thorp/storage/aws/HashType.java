package net.kemitix.thorp.storage.aws;

public class HashType extends net.kemitix.thorp.domain.HashType {
    public static net.kemitix.thorp.domain.HashType ETag = new HashType("ETag");
    protected HashType(String label) {
        super(label);
    }
}
