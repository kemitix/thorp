package net.kemitix.thorp.domain;

public class SizeTranslation {
    static long kbLimit = 10240L;
    static long mbLimit = kbLimit * 1024;
    static long gbLimit = mbLimit * 1024;
    public static String sizeInEnglish(long length) {
        double bytes = length;
        if (length > gbLimit) {
            return String.format("%.3fGb", bytes / 1024 / 1024 / 1024);
        }
        if (length > mbLimit) {
            return String.format("%.2fMb", bytes / 1024 / 1024);
        }
        if (length > kbLimit) {
            return String.format("%.0fKb", bytes / 1024);
        }
        return String.format("%db", length);
    }
}
