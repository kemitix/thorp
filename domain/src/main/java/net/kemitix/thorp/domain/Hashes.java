package net.kemitix.thorp.domain;

import net.kemitix.mon.TypeAlias;

import java.util.*;

public class Hashes extends TypeAlias<Map<HashType, MD5Hash>> {
    private Hashes() {
        super(new HashMap<>());
    }
    public static Hashes create() {
        return new Hashes();
    }
    public static Hashes create(HashType key, MD5Hash value) {
        Hashes hashes = Hashes.create();
        hashes.getValue().put(key, value);
        return hashes;
    }

    public static Hashes mergeAll(List<Hashes> hashesList) {
        Hashes hashes = Hashes.create();
        Map<HashType, MD5Hash> values = hashes.getValue();
        hashesList.stream().map(TypeAlias::getValue).forEach(values::putAll);
        return hashes;
    }

    public Hashes withKeyValue(HashType key, MD5Hash value) {
        Hashes hashes = Hashes.create();
        hashes.getValue().putAll(getValue());
        hashes.getValue().put(key, value);
        return hashes;
    }
    public Set<HashType> keys() {
        return getValue().keySet();
    }
    public Collection<MD5Hash> values() {
        return getValue().values();
    }
    public Optional<MD5Hash> get(HashType key) {
        return Optional.ofNullable(getValue().get(key));
    }
    public Hashes merge(Hashes other) {
        Hashes hashes = Hashes.create();
        hashes.getValue().putAll(getValue());
        hashes.getValue().putAll(other.getValue());
        return hashes;
    }
}
