package cn.powernukkitx.powerdoc;

import cn.powernukkitx.powerdoc.config.Arg;
import cn.powernukkitx.powerdoc.config.BookConfig;
import cn.powernukkitx.powerdoc.config.MultiLanguageDocuments;
import cn.powernukkitx.powerdoc.config.NullableArg;
import cn.powernukkitx.powerdoc.processor.Processor;
import cn.powernukkitx.powerdoc.render.Step;
import cn.powernukkitx.powerdoc.utils.FileUtils;
import cn.powernukkitx.powerdoc.utils.StringUtils;
import cn.powernukkitx.powerdoc.utils.Timing;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static cn.powernukkitx.powerdoc.utils.JsonUtils.toBasic;

public final class Book {
    public static final WeakHashMap<File, JsonObject> dirCatalogueConfigCache = new WeakHashMap<>();
    public static final Map<File, MultiLanguageDocuments> fileMultiLanguageMap = new HashMap<>();

    private final BookConfig config;
    private final File bookDir;

    private final String outputPath;
    private final Pattern pageFileFilterPattern;

    private static final WeakHashMap<String, Constructor<?>> constructorCache = new WeakHashMap<>();

    public Book(Path bookConfigPath) throws IOException {
        var content = Files.readString(bookConfigPath);
        config = BookConfig.from(JsonParser.parseString(content).getAsJsonObject());
        outputPath = config.workflow().outputPath();
        pageFileFilterPattern = Pattern.compile(config.pages().filter());
        bookDir = FileUtils.of(config.pages().path());
    }

    public BookConfig getConfig() {
        return config;
    }

    public void build() {
        preWalk();
        processFlow();
        workflow();
    }

    public void preWalk() {
        try (final var pathStream = Files.walk(bookDir.toPath())) {
            pathStream.forEach(path -> {
                final var file = path.toFile();
                final var dirConfig = getDirConfig(file.getParentFile());
                if (dirConfig != null && dirConfig.has("multiLanguage")) {
                    final var jsonArray = dirConfig.get("multiLanguage").getAsJsonArray();
                    final var multiLanguageDocuments = new MultiLanguageDocuments(jsonArray);
                    fileMultiLanguageMap.put(file, multiLanguageDocuments);
                }
            });
        } catch (IOException e) {
            Logger.getLogger("cn.powernukkitx.powerdoc").log(Level.WARNING, "Cannot walk " + bookDir.getName() + " because: " + e.getMessage());
        }
    }

    public void processFlow() {
        for (final var processorConfig : config.processFlow().processors()) {
            var processor = constructObject(Processor::getProcessorClass, processorConfig.use(), processorConfig.args());
            if (processor != null) {
                processor.work(this);
            }
        }
    }

    public void workflow() {
        buildForDir(outputPath + "/", bookDir, true);
    }

    private void buildForDir(String prefix, File dir, boolean top) {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        var pageFiles = dir.listFiles((dir1, name) ->
                !name.contains(".") || pageFileFilterPattern.matcher(name).matches());
        if (pageFiles == null || pageFiles.length == 0) {
            return;
        }
        var stream = Arrays.stream(pageFiles);
        if (pageFiles.length >= 4) {
            stream = stream.parallel();
        }
        stream.forEach(each -> {
            if (each.isFile()) {
                final var timing = new Timing();
                final var doc = new Document(each.toPath(), this);
                try {
                    doc.setText(Files.readString(doc.getSource()));
                } catch (IOException e) {
                    Logger.getLogger("cn.powernukkitx.powerdoc").log(Level.WARNING, "Cannot read " + doc.getSource() + " because: " + e.getMessage());
                    return;
                }
                // 设置基本变量
                {
                    for (final var varEntry : config.defaultVariables().entrySet()) {
                        doc.setVariable(varEntry.getKey(), varEntry.getValue());
                    }
                    doc.setVariable("file.name", each.getName());
                    doc.setVariable("file.noExtName", StringUtils.beforeLast(each.getName(), "."));
                    doc.setVariable("time.lastModified", LocalDateTime.ofEpochSecond(each.lastModified(), 0, ZoneOffset.UTC));
                    doc.setVariable("file.rootRelativePrefix", "../".repeat(bookDir.toPath().relativize(each.toPath()).getNameCount() - 1));
                    // 多语言文档处理
                    var multiLanguageDocuments = fileMultiLanguageMap.get(each);
                    if (multiLanguageDocuments != null) {
                        doc.setVariable("file.multiLanguageDocuments", multiLanguageDocuments);
                    } else {
                        final var dirConfig = getDirConfig(each.getParentFile());
                        if (dirConfig != null && dirConfig.has("multiLanguage")) {
                            final var jsonArray = dirConfig.get("multiLanguage").getAsJsonArray();
                            multiLanguageDocuments = new MultiLanguageDocuments(jsonArray);
                            fileMultiLanguageMap.put(each, multiLanguageDocuments);
                            doc.setVariable("file.multiLanguageDocuments", multiLanguageDocuments);
                        }
                    }
                    // 设置文档语言
                    if (multiLanguageDocuments != null && multiLanguageDocuments.fileName2DocumentMap.containsKey(each.getName())) {
                        final var languageId = multiLanguageDocuments.fileName2DocumentMap.get(each.getName()).language();
                        doc.setVariable("file.language", languageId);
                        doc.setVariable("file.title", getFileTitle(each, languageId));
                    }
                    // 设置文档输出路径
                    var name = each.getName();
                    name = name.substring(0, name.lastIndexOf(".")) + "." + doc.getVariable("ext", String.class, "txt");
                    final var file = FileUtils.of(prefix + (top ? "" : ("/" + dir.getName())) + "/" + name);
                    final var processedPathStr = doc.processVar(file.getAbsolutePath()); // 处理路径中的变量
                    doc.setVariable("file.outputPath", processedPathStr);
                }
                for (final var stepConfig : config.workflow().steps()) {
                    var step = constructObject(Step::getStepClass, stepConfig.use(), stepConfig.args());
                    if (step != null) {
                        step.work(doc);
                    }
                }
                var name = each.getName();
                name = name.substring(0, name.lastIndexOf(".")) + "." + doc.getVariable("ext", String.class, "txt");
                try {
                    // TODO: 2022/3/5 清理这堆屎山
                    var file = FileUtils.of(prefix + (top ? "" : ("/" + dir.getName())) + "/" + name);
                    final var processedPathStr = doc.processVar(file.getAbsolutePath()); // 处理路径中的变量
                    file = new File(processedPathStr);
                    //noinspection ResultOfMethodCallIgnored
                    file.getParentFile().mkdirs();
                    Files.writeString(file.toPath(), doc.getText());
                    timing.log(Logger.getLogger("cn.powernukkitx.powerdoc"), file.getName());
                } catch (IOException e) {
                    Logger.getLogger("cn.powernukkitx.powerdoc").log(Level.FINE, "Cannot generate " + Path.of(prefix, dir.getName(), name) + " because: " + e.getLocalizedMessage());
                }
            } else if (each.isDirectory()) {
                if (config.pages().recursion())
                    buildForDir(top ? (prefix) : (prefix + "/" + dir.getName()), each, false);
            }
        });
    }

    private <T> T constructObject(Function<String, Class<T>> provider, String id, Map<String, Object> args) {
        final var uniqueConstructorIdBuilder = new StringBuilder(id);
        args.keySet().forEach(s -> uniqueConstructorIdBuilder.append("&").append(s));
        final var uniqueConstructorId = uniqueConstructorIdBuilder.toString();

        Constructor<?>[] candidateConstructors;

        var clazz = provider.apply(id);
        if (clazz == null) {
            Logger.getLogger("cn.powernukkitx.powerdoc").log(Level.WARNING, "Step " + id + " not found.");
            return null;
        }
        if (constructorCache.containsKey(uniqueConstructorId)) {
            candidateConstructors = new Constructor[]{constructorCache.get(uniqueConstructorId)};
        } else {
            candidateConstructors = clazz.getConstructors();
        }

        outer:
        for (final var constructor : candidateConstructors) {
            var usedArgs = 0;
            var constructorArguments = new ArrayList<>(constructor.getParameterCount());
            for (final var parameter : constructor.getParameters()) {
                var argAnnotation = parameter.getAnnotation(Arg.class);
                if (argAnnotation != null) {
                    var value = args.get(argAnnotation.value());
                    if (value == null) {
                        var nullAnnotation = parameter.getAnnotation(NullableArg.class);
                        if (nullAnnotation != null) {
                            constructorArguments.add(null);
                        } else {
                            continue outer;
                        }
                    } else {
                        // TODO: 2022/2/26 JsonArray和JsonObject转为List和Map
                        var basic = value instanceof JsonElement jsonElement ? toBasic(jsonElement) : value;
                        if (parameter.getType().isInstance(basic)) {
                            usedArgs++;
                            constructorArguments.add(basic);
                        } else {
                            continue outer;
                        }
                    }
                } else {
                    continue outer;
                }
            }
            if (usedArgs == args.size()) {
                try {
                    constructorCache.put(uniqueConstructorId, constructor);
                    return clazz.cast(constructor.newInstance(constructorArguments.toArray()));
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException ignore) {

                }
            }
        }
        Logger.getLogger("cn.powernukkitx.powerdoc").log(Level.WARNING, "Cannot build " + id + " with " + args + " .");
        return null;
    }

    public static JsonObject getDirConfig(File dir) {
        var configJson = Book.dirCatalogueConfigCache.get(dir);
        if (configJson == null) {
            final var dirFile = new File(dir, "dir.json");
            if (dirFile.exists()) {
                try {
                    configJson = JsonParser.parseString(Files.readString(dirFile.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
                } catch (Exception ignore) {

                }
            }
        }
        if (configJson != null) {
            Book.dirCatalogueConfigCache.put(dir, configJson);
        }
        return configJson;
    }

    public static String getFileTitle(File file, String languageId) {
        final var parentFile = file.getParentFile();
        var configJson = Book.getDirConfig(parentFile);
        if (configJson == null) {
            return StringUtils.beforeLast(file.getName(), ".");
        }
        if (configJson.has("title")) {
            final var displayNameJson = configJson.get("title").getAsJsonObject();
            if (displayNameJson.has(file.getName())) {
                final var displayNameEntry = displayNameJson.get(file.getName());
                if (displayNameEntry instanceof JsonPrimitive primitive) {
                    return primitive.getAsString();
                } else if (displayNameEntry instanceof JsonObject jsonObject) {
                    if (jsonObject.has(languageId)) {
                        return jsonObject.get(languageId).getAsString();
                    }
                }
            }
        }
        return StringUtils.beforeLast(file.getName(), ".");
    }
}
