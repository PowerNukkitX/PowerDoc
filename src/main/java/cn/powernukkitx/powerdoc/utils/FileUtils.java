package cn.powernukkitx.powerdoc.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

public final class FileUtils {
    public static void copy(final Path source, final Path destination, final Consumer<IOException> IOEHandler) {
        try (var fileStream = Files.walk(source)) {
            fileStream.parallel()
                    .map(each -> new DataUtils.Pair<>(each, source.relativize(each)))
                    .map(pair -> pair.setB(destination.resolve(pair.getB())))
                    .forEach(pair -> {
                        try {
                            Files.createDirectories(pair.getB().getParent());
                            Files.copy(pair.getA(), pair.getB(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (DirectoryNotEmptyException ignore) {

                        } catch (IOException e) {
                            IOEHandler.accept(e);
                        }
                    });
        } catch (IOException e) {
            IOEHandler.accept(e);
        }
    }

    public static File of(String path) {
        return new File(PathUtils.workingPath.toFile(), path);
    }

    /**
     * target相对于source的路径
     *
     * @param source File
     * @param target File
     * @return target相对于source的路径
     */
    public static String relativeWebPath(File source, File target) {
        if (source.equals(target)) {
            return "#";
        }
        final var sourceDir = source.isDirectory() ? source : source.getParentFile();
        final var targetDir = target.isDirectory() ? target : target.getParentFile();
        if (sourceDir.equals(targetDir)) {
            return target.getName();
        }
        return (sourceDir.toPath().relativize(targetDir.toPath()) + "/" + target.getName()).replace("\\", "/");
    }
}
