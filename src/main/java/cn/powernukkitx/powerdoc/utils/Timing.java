package cn.powernukkitx.powerdoc.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

public record Timing(long currentTimeMillis) {
    public Timing() {
        this(System.currentTimeMillis());
    }

    public long ends() {
        return System.currentTimeMillis() - currentTimeMillis;
    }

    public void log(Logger logger, String item) {
        logger.log(Level.FINE, item + " : " + ends() + "ms");
    }
}
