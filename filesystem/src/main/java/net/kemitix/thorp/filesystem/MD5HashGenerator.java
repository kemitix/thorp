package net.kemitix.thorp.filesystem;

import net.kemitix.thorp.domain.HashGenerator;
import net.kemitix.thorp.domain.HashType;
import net.kemitix.thorp.domain.Hashes;
import net.kemitix.thorp.domain.MD5Hash;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5HashGenerator implements HashGenerator {
    private static final int maxBufferSize = 8048;
    private static final byte[] defaultBuffer = new byte[maxBufferSize];
    public static String hex(byte[] in) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(in);
        return MD5Hash.digestAsString(md5.digest());
    }
    public static byte[] digest(String in) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(in.getBytes());
        return md5.digest();
    }
    @Override
    public String hashFile(Path path) throws IOException, NoSuchAlgorithmException {
        return md5File(path).hash();
    }

    @Override
    public Hashes hash(Path path) throws IOException, NoSuchAlgorithmException {
        HashType key = hashType();
        MD5Hash value = MD5HashGenerator.md5File(path);
        return Hashes.create(key, value);
    }

    @Override
    public MD5Hash hashChunk(Path path, Long index, long partSize) throws IOException, NoSuchAlgorithmException {
        return md5FileChunk(path, index, partSize);
    }

    public static MD5Hash md5File(Path path) throws IOException, NoSuchAlgorithmException {
        return md5FileChunk(path, 0, path.toFile().length());
    }
    public static MD5Hash md5FileChunk(Path path, long offset, long size) throws IOException, NoSuchAlgorithmException {
        File file = path.toFile();
        long endOffset = Math.min(offset + size, file.length());
        byte[] digest = readFile(file, offset, endOffset);
        return MD5Hash.fromDigest(digest);
    }
    public static byte[] readFile(File file, long offset, long endOffset) throws IOException, NoSuchAlgorithmException {
        try(FileInputStream fis = openAtOffset(file, offset)) {
            return digestFile(fis, offset, endOffset);
        }
    }
    private static FileInputStream openAtOffset(File file, long offset) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        long skippedBytes = fileInputStream.skip(offset);
        if (skippedBytes != offset) {
            throw new RuntimeException("Failed to skip within file: " + file.toString());
        }
        return fileInputStream;
    }
    private static byte[] digestFile(FileInputStream fis, long offset, long endOffset) throws NoSuchAlgorithmException, IOException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        for (long currentOffset = offset; currentOffset <  endOffset; currentOffset += maxBufferSize) {
            md5.update(readToBuffer(fis, currentOffset, endOffset));
        }
        return md5.digest();
    }

    private static byte[] readToBuffer(FileInputStream fis, long currentOffset, long endOffset) throws IOException {
        int nextBufferSize = nextBufferSize(currentOffset, endOffset);
        byte[] buffer = nextBufferSize < maxBufferSize
                ? new byte[nextBufferSize]
                : defaultBuffer;
        int bytesRead = fis.read(buffer);
        return buffer;
    }

    private static int nextBufferSize(long currentOffset, long endOffset) {
        long toRead = endOffset - currentOffset;
        return (int) Math.min(maxBufferSize, toRead);
    }

    @Override
    public HashType hashType() {
        return HashType.MD5;
    }

    @Override
    public String label() {
        return "MD5";
    }
}
