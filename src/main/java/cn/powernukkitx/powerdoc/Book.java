package cn.powernukkitx.powerdoc;

import cn.powernukkitx.powerdoc.config.Arg;
import cn.powernukkitx.powerdoc.config.BookConfig;
import cn.powernukkitx.powerdoc.config.NullableArg;
import cn.powernukkitx.powerdoc.processor.Processor;
import cn.powernukkitx.powerdoc.render.Document;
import cn.powernukkitx.powerdoc.render.Step;
import cn.powernukkitx.powerdoc.utils.StringUtils;
import cn.powernukkitx.powerdoc.utils.Timing;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static cn.powernukkitx.powerdoc.utils.JsonUtils.toBasic;

public final class Book {
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
        bookDir = new File(config.pages().path());
    }

    public BookConfig getConfig() {
        return config;
    }

    public void build() {
        processFlow();
        workflow();
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
        var pageFiles = dir.listFiles((dir1, name) -> pageFileFilterPattern.matcher(name).matches());
        if (pageFiles == null || pageFiles.length == 0) {
            return;
        }
        var stream = Arrays.stream(pageFiles);
        if(pageFiles.length >= 4) {
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
                    doc.setVariable("file.name", each.getName());
                    doc.setVariable("file.noExtName", StringUtils.beforeLast(each.getName(), "."));
                    doc.setVariable("time.lastModified", LocalDateTime.ofEpochSecond(each.lastModified(), 0, ZoneOffset.UTC));
                    doc.setVariable("file.rootRelativePrefix", "../".repeat(bookDir.toPath().relativize(each.toPath()).getNameCount() - 1));
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
                    var file = new File(prefix + (top ? "" : ("/" + dir.getName())) + "/" + name);
                    //noinspection ResultOfMethodCallIgnored
                    file.getParentFile().mkdirs();
                    Files.writeString(file.toPath(), doc.getText());
                    timing.log(Logger.getLogger("cn.powernukkitx.powerdoc"), file.getName());
                } catch (IOException e) {
                    Logger.getLogger("cn.powernukkitx.powerdoc").log(Level.FINE, "Cannot generate " + Path.of(prefix, dir.getName(), name) + " because: " + e.getMessage());
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
        if(constructorCache.containsKey(uniqueConstructorId)) {
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
}
