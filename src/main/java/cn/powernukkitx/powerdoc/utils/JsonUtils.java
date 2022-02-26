package cn.powernukkitx.powerdoc.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.HashMap;
import java.util.Map;

public final class JsonUtils {
    public static Map<String, Object> toMap(JsonObject jsonObject) {
        var map = new HashMap<String, Object>(jsonObject.size());
        for (final var each : jsonObject.entrySet()) {
            map.put(each.getKey(), toBasic(each.getValue()));
        }
        return map;
    }

    public static Object toBasic(JsonElement jsonElement) {
        if (jsonElement instanceof JsonNull) {
            return null;
        } else if (jsonElement instanceof JsonPrimitive primitive) {
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            } else if (primitive.isString()) {
                return primitive.getAsString();
            } else if (primitive.isNumber()) {
                return primitive.getAsInt();
            }
        } else if(jsonElement instanceof JsonObject object) {
            return toMap(object);
        }
        return jsonElement;
    }

}
