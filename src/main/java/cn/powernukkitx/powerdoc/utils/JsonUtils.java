package cn.powernukkitx.powerdoc.utils;

import com.google.gson.*;

import java.lang.reflect.Array;
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

    public static Object[] toArray(JsonArray jsonArray) {
        var out = new Object[jsonArray.size()];
        Class<?> previousClass = null;
        Object tmp;
        for (int i = 0, length = jsonArray.size(); i < length; i++) {
            tmp = toBasic(jsonArray.get(i));
            if (tmp == null) {
                continue;
            }
            out[i] = tmp;
            if (previousClass == null) {
                previousClass = tmp.getClass();
            } else if (!tmp.getClass().isAssignableFrom(previousClass)) {
                if (previousClass.isAssignableFrom(tmp.getClass())) {
                    previousClass = tmp.getClass();
                } else {
                    previousClass = Object.class;
                }
            }
        }
        if (previousClass != Object.class) {
            var arr = Array.newInstance(previousClass, out.length);
            //noinspection SuspiciousSystemArraycopy
            System.arraycopy(out, 0, arr, 0, out.length);
            return (Object[]) arr;
        }
        return out;
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
        } else if (jsonElement instanceof JsonObject object) {
            return toMap(object);
        } else if (jsonElement instanceof JsonArray array) {
            return toArray(array);
        }
        return jsonElement;
    }

}
