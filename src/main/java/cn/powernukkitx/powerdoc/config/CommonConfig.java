package cn.powernukkitx.powerdoc.config;

import com.google.gson.JsonObject;

public final class CommonConfig {
    public record FileCollection(String path, boolean recursion, String filter) {
        public static FileCollection from(JsonObject jsonObject) {
            return new FileCollection(jsonObject.get("path").getAsString(), jsonObject.get("recursion").getAsBoolean(), jsonObject.get("filter").getAsString());
        }
    }
}
