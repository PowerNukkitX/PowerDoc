package cn.powernukkitx.powerdoc.render;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class MarkdownStep implements Step {
    public static final Parser markdownParser;
    public static final HtmlRenderer htmlRenderer;

    static {
        markdownParser = Parser.builder().build();
        htmlRenderer = HtmlRenderer.builder().build();
    }

    @Override
    public String getName() {
        return "MarkdownStep";
    }

    @Override
    public String getId() {
        return "markdown";
    }

    @Override
    public void work(Document document) {
        var rootNode = markdownParser.parse(document.getText());
        document.setVariable("markdown.rendered", htmlRenderer.render(rootNode));
    }
}
