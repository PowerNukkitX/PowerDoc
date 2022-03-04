package cn.powernukkitx.powerdoc.utils;

import java.io.File;
import java.nio.file.Path;

public final class PathUtils {
    public static Path workingPath = Path.of(new File("./").getAbsolutePath());

    public static Path relative(String path) {
        return relative(Path.of(path));
    }

    public static Path relative(Path path) {
        return workingPath.relativize(path);
    }

    public static String relativeStr(String path) {
        return relative(path).toString();
    }

    public static String relativeStr(Path path) {
        return relative(path).toString();
    }
}
