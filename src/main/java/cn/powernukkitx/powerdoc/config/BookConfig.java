package cn.powernukkitx.powerdoc.config;

import cn.powernukkitx.powerdoc.utils.JsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

import static cn.powernukkitx.powerdoc.utils.JsonUtils.toMap;

public record BookConfig(String title, String[] author, Map<String, Object> defaultVariables, CommonConfig.FileCollection pages, BookProcessFlow processFlow,
                         BookWorkflow workflow) {

    public static BookConfig from(JsonObject jsonObject) {
        String[] author;
        var tmp = jsonObject.get("author");
        if (tmp instanceof JsonArray array) {
            author = new String[array.size()];
            for (int i = 0, length = author.length; i < length; i++) {
                author[i] = array.get(i).toString();
            }
        } else {
            author = new String[]{tmp.getAsString()};
        }
        return new BookConfig(jsonObject.get("title").getAsString(), author,
                JsonUtils.toMap(jsonObject.get("defaultVariables").getAsJsonObject()),
                CommonConfig.FileCollection.from(jsonObject.get("pages").getAsJsonObject()),
                BookProcessFlow.from(jsonObject.get("processflow").getAsJsonObject()),
                BookWorkflow.from(jsonObject.get("workflow").getAsJsonObject()));
    }

    public record BookProcessFlow(BookProcessor[] processors) {
        public static BookProcessFlow from(JsonObject jsonObject) {
            var arr = jsonObject.get("processors").getAsJsonArray();
            var processors = new BookProcessor[arr.size()];
            for (int i = 0, length = arr.size(); i < length; i++) {
                processors[i] = BookProcessor.from(arr.get(i).getAsJsonObject());
            }
            return new BookProcessFlow(processors);
        }
    }

    public record BookProcessor(String id, String use, Map<String, Object> args) {
        public static BookProcessor from(JsonObject jsonObject) {
            var obj = jsonObject.get("args");
            return new BookProcessor(jsonObject.get("id").getAsString(), jsonObject.get("use").getAsString(),
                    obj == null ? new HashMap<>(0) : toMap(obj.getAsJsonObject()));
        }
    }

    public record BookWorkflow(String outputPath, BookWorkflowStep[] steps) {
        public static BookWorkflow from(JsonObject jsonObject) {
            var arr = jsonObject.get("steps").getAsJsonArray();
            var steps = new BookWorkflowStep[arr.size()];
            for (int i = 0, length = arr.size(); i < length; i++) {
                steps[i] = BookWorkflowStep.from(arr.get(i).getAsJsonObject());
            }
            return new BookWorkflow(jsonObject.get("outputPath").getAsString(), steps);
        }
    }

    public record BookWorkflowStep(String id, String use, Map<String, Object> args) {
        public static BookWorkflowStep from(JsonObject jsonObject) {
            var obj = jsonObject.get("args");
            return new BookWorkflowStep(jsonObject.get("id").getAsString(), jsonObject.get("use").getAsString(),
                    obj == null ? new HashMap<>(0) : toMap(obj.getAsJsonObject()));
        }
    }
}
