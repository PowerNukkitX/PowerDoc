package cn.powernukkitx.powerdoc;

import cn.powernukkitx.powerdoc.render.DocumentStepRecord;
import cn.powernukkitx.powerdoc.utils.DataUtils;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Document implements Serializable {
    private final Path source;
    private final Book book;
    private String text;
    private final Map<String, DocumentStepRecord> stepRecords = new HashMap<>();
    private final Map<String, Object> variablesMap = new HashMap<>();

    public Document(Path source, Book book) {
        this.source = source;
        this.book = book;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    public void addStepRecord(final DocumentStepRecord stepRecord) {
        this.stepRecords.put(stepRecord.stepName(), stepRecord);
    }

    public boolean hasBeenRendBy(final String stepName) {
        return stepRecords.containsKey(stepName);
    }

    public Path getSource() {
        return source;
    }

    public Object getVariable(String key) {
        return variablesMap.get(key);
    }

    public Set<Map.Entry<String, Object>> getVariables() {
        return variablesMap.entrySet();
    }

    public <T> T getVariable(String key, Class<T> clazz) {
        var obj = variablesMap.get(key);
        if (clazz.isInstance(obj)) {
            return clazz.cast(obj);
        }
        return null;
    }

    public <T> T getVariable(String key, Class<T> clazz, T defaultValue) {
        var obj = variablesMap.get(key);
        if (clazz.isInstance(obj)) {
            return clazz.cast(obj);
        }
        return defaultValue;
    }

    public void setVariable(String key, Object value) {
        variablesMap.put(key, value);
    }

    @SafeVarargs
    public final String processVar(String str, DataUtils.Pair<String, String>... tmpVars) {
        if (str.contains("%")) {
            for (final var pair : tmpVars) {
                str = str.replace("%" + pair.getA() + "%", pair.getB());
            }
            for (final var varEntry : this.getVariables()) {
                str = str.replace("%" + varEntry.getKey() + "%", varEntry.getValue().toString());
            }
        }
        return str;
    }

    public Book getBook() {
        return book;
    }
}
