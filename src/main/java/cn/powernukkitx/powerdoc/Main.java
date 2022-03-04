package cn.powernukkitx.powerdoc;

import cn.powernukkitx.powerdoc.utils.Timing;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) {
        try {
            final var logger = Logger.getLogger("cn.powernukkitx.powerdoc");
            final var timing = new Timing();
            var file = new File(args[0]);
            var dir = file.getParentFile();
            if (dir == null) {
                dir = new File("./");
            }
            logger.setLevel(Level.INFO);
            System.setProperty("user.dir", dir.getAbsolutePath());
            new Book(file.toPath()).build();
            logger.log(Level.INFO, "Build finished in " + timing.ends() + "ms.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
