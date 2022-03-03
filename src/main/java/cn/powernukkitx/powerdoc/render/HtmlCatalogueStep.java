package cn.powernukkitx.powerdoc.render;

import cn.powernukkitx.powerdoc.config.Arg;
import cn.powernukkitx.powerdoc.config.BookConfig;
import cn.powernukkitx.powerdoc.config.Exposed;
import cn.powernukkitx.powerdoc.config.NullableArg;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static cn.powernukkitx.powerdoc.utils.StringUtils.beforeLast;

public class HtmlCatalogueStep implements Step {
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
        final var bookDir = new File(book.getConfig().pages().path());
        html = html.formatted(navId, navCssClass, listDivCssClass, makeCatalogue(book.getConfig(),
                pattern, bookDir, document.getSource().toFile(), 0));
        document.setVariable("html.catalogue", html);
    }

    private String makeCatalogue(BookConfig config, Pattern pageFileFilterPattern, File dir, File workingFor, int depth) {
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
                final var href = relativeWebPath(workingFor, each);
                if ("#".equals(href)) {
                    out.append("""
                            <li class="%s"><strong class="%s">%s</strong></li>
                            """.formatted(liCssClass, strongCssClass, beforeLast(each.getName(), ".")));
                } else {
                    out.append("""
                            <li class="%s"><a class="%s" href="%s">%s</a></li>
                            """.formatted(liCssClass, aCssClass, beforeLast(href, ".") + ".html", beforeLast(each.getName(), ".")));
                }
            } else if (each.isDirectory()) {
                if (config.pages().recursion()) {
                    out.append(makeCatalogue(config, pageFileFilterPattern, each, workingFor, depth + 1));
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

    /**
     * target相对于source的路径
     *
     * @param source File
     * @param target File
     * @return target相对于source的路径
     */
    private static String relativeWebPath(File source, File target) {
        if(source.equals(target)) {
            return "#";
        }
        final var sourceDir = source.isDirectory() ? source : source.getParentFile();
        final var targetDir = target.isDirectory() ? target : target.getParentFile();
        if (sourceDir.equals(targetDir)) {
            return target.getName();
        }
        return (sourceDir.toPath().relativize(targetDir.toPath()) + "/" + target.getName()).replace("\\", "/");
    }
}
