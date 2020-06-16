package net.kemitix.thorp.filesystem;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.kemitix.thorp.domain.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PathCache {
    private final Map<Path, FileData> data;
    public Optional<FileData> get(Path path) {
        return Optional.ofNullable(data.get(path));
    }

    public static final String fileName = ".thorp.cache";
    public static final String tempFileName = fileName + ".tmp";
    public static PathCache create(Map<Path, FileData> data) {
        return new PathCache(data);
    }
    public static Set<String> export(Path path, FileData fileData) {
        return fileData.hashes
                .keys()
                .stream()
                .map(hashType ->
                        fileData.hashes.get(hashType)
                                .map(MD5Hash::hash)
                                .map(hashHash -> String.join(":",
                                        hashType.label,
                                        hashHash,
                                        Long.toString(fileData.lastModified
                                                .at()
                                                .toEpochMilli()),
                                        path.toString()
                                        )))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }
    private static final String pattern =
            "^(?<hashtype>.+):(?<hash>.+):(?<modified>\\d+):(?<filename>.+)$";
    private static final Pattern format = Pattern.compile(pattern);
    public static PathCache fromLines(List<String> lines) {
        return PathCache.create(
                lines.stream()
                        .map(format::matcher)
                        .filter(Matcher::matches)
                        .map(matcher -> Tuple.create(
                                Paths.get(matcher.group("filename")),
                                FileData.create(
                                        getHashes(matcher),
                                        getModified(matcher)
                                ))).collect(Collectors.toMap(
                        tuple -> tuple.a,// keymapper - path
                        tuple -> tuple.b,// value mapper - file data
                        FileData::join)));// merge function
    }

    private static LastModified getModified(Matcher matcher) {
        return LastModified.at(
                Instant.ofEpochMilli(
                        Long.parseLong(
                                matcher.group("modified"))));
    }

    private static Hashes getHashes(Matcher matcher) {
        return Hashes.create(
                getHashtype(matcher),
                MD5Hash.create(matcher.group("hash")));
    }

    private static HashType getHashtype(Matcher matcher) {
        return HashGenerator.generatorFor(matcher.group("hashtype"))
                .hashType();
    }

}
