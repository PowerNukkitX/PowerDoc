package cn.powernukkitx.powerdoc.render;

import cn.powernukkitx.powerdoc.Book;
import cn.powernukkitx.powerdoc.config.Arg;
import cn.powernukkitx.powerdoc.config.BookConfig;
import cn.powernukkitx.powerdoc.config.Exposed;
import cn.powernukkitx.powerdoc.config.NullableArg;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

import static cn.powernukkitx.powerdoc.utils.StringUtils.beforeLast;

public class HtmlCatalogueStep implements Step {
    public static final Map<Book, String> cache = new WeakHashMap<>();

    protected final String navCssClass;
    protected final String divCssClass;
    protected final String listDivCssClass;
    protected final String olCssClass;
    protected final String liCssClass;
    protected final String aCssClass;
    protected final String pCssClass;

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
        if (cache.containsKey(book)) {
            document.setVariable("html.catalogue", cache.get(book));
        } else {
            var html = """
                    <nav id="%s" class="%s"><div class="%s">%s</div></nav>""";
            final var pattern = Pattern.compile(book.getConfig().pages().filter());
            final var bookFile = new File(book.getConfig().pages().path());
            html = html.formatted(navId, navCssClass, listDivCssClass, makeCatalogue(book.getConfig(),
                    pattern, bookFile, document.getSource()));
            document.setVariable("html.catalogue", html);
            cache.put(book, html);
        }
    }

    private String makeCatalogue(BookConfig config, Pattern pageFileFilterPattern, File dir, Path currentPath) {
        var out = new StringBuilder();
        if (!dir.exists() || !dir.isDirectory()) {
            return "";
        }
        var pageFiles = dir.listFiles((dir1, name) -> pageFileFilterPattern.matcher(name).matches());
        if (pageFiles == null || pageFiles.length == 0) {
            return "";
        }
        for (final var each : pageFiles) {
            if (each.isFile()) {
                out.append("""
                        <li class="%s"><a class="%s" href="%s">%s</a></li>
                        """.formatted(liCssClass, aCssClass,
                        beforeLast(currentPath.relativize(each.toPath()).toString(), ".") + ".html",
                        beforeLast(each.getName(), ".")));
            } else if (each.isDirectory()) {
                if (config.pages().recursion()) {
                    out.append(makeCatalogue(config, pageFileFilterPattern, each, currentPath));
                }
            }
        }
        return """
                <ol class="%s">
                <p class="%s">%s</p>
                %s
                </ol>
                """.formatted(olCssClass, pCssClass, dir.getName(), out.toString());
    }
}
