package cn.powernukkitx.powerdoc.utils;

public final class StringUtils {
    public static String beforeLast(String str, String splitter) {
        final var index = str.lastIndexOf(splitter);
        if (index == -1) return str;
        return str.substring(0, index);
    }
}
