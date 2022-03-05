package cn.powernukkitx.powerdoc.processor;

import cn.powernukkitx.powerdoc.Book;
import cn.powernukkitx.powerdoc.config.Arg;
import cn.powernukkitx.powerdoc.utils.FileUtils;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CopyFileProcessor implements Processor {
    protected Map<String, Object> copies;

    public CopyFileProcessor(@Arg("copies") Map<String, Object> copies) {
        this.copies = copies;
    }

    @Override
    public String getName() {
        return "CopyFileProcessor";
    }

    @Override
    public String getId() {
        return "copy-file";
    }

    @Override
    public void work(Book book) {
        for (final var each : copies.entrySet()) {
            final var file = FileUtils.of(each.getKey());
            if (file.exists()) {
                final var value = each.getValue();
                if (value instanceof String str) {
                    FileUtils.copy(file.toPath(), FileUtils.of(str).toPath(), e -> Logger.getLogger("cn.powernukkitx.powerdoc")
                            .log(Level.WARNING, "Cannot copy " + each.getKey() + " because: " + e.getLocalizedMessage()));
                } else if (value instanceof String[] arr) {
                    for (final var str : arr) {
                        FileUtils.copy(file.toPath(), FileUtils.of(str).toPath(), e -> Logger.getLogger("cn.powernukkitx.powerdoc")
                                .log(Level.WARNING, "Cannot copy " + each.getKey() + " because: " + e.getLocalizedMessage()));
                    }
                }
            }
        }
    }
}
