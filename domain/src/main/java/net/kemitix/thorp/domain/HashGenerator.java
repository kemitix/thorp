package net.kemitix.thorp.domain;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public interface HashGenerator {

    HashType hashType();
    String label();
    String hashFile(Path path) throws IOException, NoSuchAlgorithmException;
    Hashes hash(Path path) throws IOException, NoSuchAlgorithmException;
    MD5Hash hashChunk(Path path, Long index, long partSize) throws IOException, NoSuchAlgorithmException;

    static List<HashGenerator> all() {
        ServiceLoader<HashGenerator> hashGenerators = ServiceLoader.load(HashGenerator.class);
        List<HashGenerator> list = new ArrayList<>();
        for(HashGenerator hashGenerator: hashGenerators) {
            list.add(hashGenerator);
        }
        return list;
    }
    static HashGenerator generatorFor(String label) {
        return all()
                .stream()
                .filter(g -> g.label().equals(label))
                .findFirst()
                .orElseThrow(() ->  new RuntimeException("Unknown hash type: " + label));
    }
    static HashType typeFrom(String label) {
        return generatorFor(label).hashType();
    }

    static Hashes hashObject(Path path) throws IOException, NoSuchAlgorithmException {
        List<Hashes> hashesList = new ArrayList<>();
        for (HashGenerator hashGenerator : all()) {
            hashesList.add(hashGenerator.hash(path));
        }
        return Hashes.mergeAll(hashesList);
    }
}
