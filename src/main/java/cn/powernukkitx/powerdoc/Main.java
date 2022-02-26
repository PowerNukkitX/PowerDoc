package cn.powernukkitx.powerdoc;

import java.io.IOException;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        try {
            new Book(Path.of(args[0])).build();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
