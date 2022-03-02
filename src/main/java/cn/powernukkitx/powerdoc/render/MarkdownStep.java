package cn.powernukkitx.powerdoc.render;

import cn.powernukkitx.powerdoc.config.Arg;
import cn.powernukkitx.powerdoc.config.Exposed;
import cn.powernukkitx.powerdoc.config.NullableArg;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.Map;

public class MarkdownStep implements Step {
    public static final Parser markdownParser;
    public final HtmlRenderer htmlRenderer;

    static {
        markdownParser = Parser.builder().build();
    }

    @Exposed
    public MarkdownStep(final @Arg("cssClass") @NullableArg Map<String, String> cssClass) {
        if (cssClass == null) {
            this.htmlRenderer = HtmlRenderer.builder().build();
        } else {
            this.htmlRenderer = HtmlRenderer.builder().attributeProviderFactory(context -> (node, tagName, attributes) -> {
                if (cssClass.containsKey(tagName))
                    attributes.put("class", cssClass.get(tagName));
            }).build();
        }
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
