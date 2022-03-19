package cn.powernukkitx.powerdoc.render;

import cn.powernukkitx.powerdoc.Book;
import cn.powernukkitx.powerdoc.Document;
import cn.powernukkitx.powerdoc.config.Arg;
import cn.powernukkitx.powerdoc.config.BookConfig;
import cn.powernukkitx.powerdoc.config.Exposed;
import cn.powernukkitx.powerdoc.config.NullableArg;
import cn.powernukkitx.powerdoc.utils.FileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

import static cn.powernukkitx.powerdoc.Book.getFileTitle;
import static cn.powernukkitx.powerdoc.utils.FileUtils.relativeWebPath;
import static cn.powernukkitx.powerdoc.utils.StringUtils.beforeLast;

public class HtmlCatalogueStep implements Step {
    private static final WeakHashMap<File, File[]> childFileCache = new WeakHashMap<>();

    protected final String navCssClass;
    protected final String divCssClass;
    protected final String listDivCssClass;
    protected final String olCssClass;
    protected final String liCssClass;
    protected final String aCssClass;
    protected final String pCssClass;
    protected final String strongCssClass;

    private final String navId;

    @Exposed
    public HtmlCatalogueStep(@Arg("navId") @NullableArg String navId, @Arg("cssClass") @NullableArg Map<String, String> cssClass) {
        if (cssClass == null) cssClass = new HashMap<>(0);
        this.navCssClass = cssClass.getOrDefault("nav", " ");
        this.divCssClass = cssClass.getOrDefault("div", " ");
        this.listDivCssClass = divCssClass + cssClass.getOrDefault("div#list", " ");
        this.olCssClass = cssClass.getOrDefault("ol", " ");
        this.liCssClass = cssClass.getOrDefault("li", " ");
        this.aCssClass = cssClass.getOrDefault("a", " ");
        this.pCssClass = cssClass.getOrDefault("p", " ");
        this.strongCssClass = cssClass.getOrDefault("strong", " ");
        this.navId = navId != null ? navId : "catalogue";
    }

    @Override
    public String getName() {
        return "HtmlCatalogueStep";
    }

    @Override
    public String getId() {
        return "catalogue";
    }

    @Override
    public void work(Document document) {
        var book = document.getBook();
        var html = """
                <nav id="%s" class="%s"><div class="%s">%s</div></nav>""";
        final var pattern = Pattern.compile(book.getConfig().pages().filter());
        final var bookDir = FileUtils.of(book.getConfig().pages().path());
        html = html.formatted(navId, navCssClass, listDivCssClass, makeCatalogue(book.getConfig(),
                document.getVariable("file.language", String.class, ""),
                pattern, bookDir, document.getSource().toFile(), 0));
        document.setVariable("html.catalogue", html);
    }

    private String makeCatalogue(BookConfig config, String languageId, Pattern pageFileFilterPattern, File dir, File workingFor, int depth) {
        var out = new StringBuilder();
        if (!dir.exists() || !dir.isDirectory()) {
            return "";
        }
        var pageFiles = childFileCache.get(dir);
        if (pageFiles == null) {
            pageFiles = dir.listFiles((dir1, name) ->
                    !name.contains(".") || pageFileFilterPattern.matcher(name).matches());
            final var dirConfig = Book.getDirConfig(dir);
            if (dirConfig != null && pageFiles != null) {
                try {
                    if (dirConfig.has("catalogue")) {
                        final var catalogueJson = dirConfig.get("catalogue").getAsJsonObject();
                        if (catalogueJson.has("priority")) {
                            final var priority = catalogueJson.get("priority").getAsJsonObject();
                            Arrays.sort(pageFiles, (a, b) -> {
                                final var ta = priority.get(a.getName());
                                final var pa = ta == null ? 0 : ta.getAsInt();
                                final var tb = priority.get(b.getName());
                                final var pb = tb == null ? 0 : tb.getAsInt();
                                return pb - pa;
                            });
                        }
                    }
                } catch (Exception ignore) {

                }
            }
        }
        if (pageFiles == null || pageFiles.length == 0) {
            return "";
        }
        childFileCache.put(dir, pageFiles);
        for (final var each : pageFiles) {
            if (each.isFile()) {
                final var multiLanguageDocuments = Book.fileMultiLanguageMap.get(each);
                if (multiLanguageDocuments != null) {
                    final var targetDocument = multiLanguageDocuments.fileName2DocumentMap.get(each.getName());
                    if (targetDocument != null && !languageId.equals(targetDocument.language())) {
                        continue;
                    }
                }
                final var href = relativeWebPath(workingFor, each);
                if ("#".equals(href)) {
                    out.append("""
                            <li class="%s"><strong class="%s">%s</strong></li>
                            """.formatted(liCssClass, strongCssClass, getFileTitle(each, languageId)));
                } else {
                    out.append("""
                            <li class="%s"><a class="%s" href="%s">%s</a></li>
                            """.formatted(liCssClass, aCssClass, beforeLast(href, ".") + ".html", getFileTitle(each, languageId)));
                }
            } else if (each.isDirectory()) {
                if (config.pages().recursion()) {
                    out.append("""
                                    <li class="%s"><p class="%s">%s</p></li>
                                    """.formatted(liCssClass, pCssClass, getFileTitle(each, languageId)))
                            .append(makeCatalogue(config, languageId, pageFileFilterPattern, each, workingFor, depth + 1));
                }
            }
        }
        return """
                <ol class="%s">
                %s
                </ol>
                """.formatted(olCssClass, out.toString());
    }
}
