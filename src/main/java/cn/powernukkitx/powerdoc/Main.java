package cn.powernukkitx.powerdoc;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            var file = new File(args[0]);
            var dir = file.getParentFile();
            if(dir == null) {
                dir = new File("./");
            }
            System.setProperty("user.dir", dir.getAbsolutePath());
            new Book(file.toPath()).build();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
