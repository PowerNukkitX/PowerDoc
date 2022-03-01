package cn.powernukkitx.powerdoc.utils;

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
}
