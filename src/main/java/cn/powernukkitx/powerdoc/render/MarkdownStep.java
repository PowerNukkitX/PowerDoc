package cn.powernukkitx.powerdoc.render;

import cn.powernukkitx.powerdoc.Document;
import cn.powernukkitx.powerdoc.config.Arg;
import cn.powernukkitx.powerdoc.config.Exposed;
import cn.powernukkitx.powerdoc.config.NullableArg;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.ext.image.attributes.ImageAttributesExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.List;
import java.util.Map;

public class MarkdownStep implements Step {
    public static final Parser markdownParser;
    public final HtmlRenderer htmlRenderer;
    public static final List<Extension> extensionList;

    static {
        extensionList = List.of(StrikethroughExtension.create(),
                TablesExtension.create(),
                HeadingAnchorExtension.create(),
                ImageAttributesExtension.create(),
                TaskListItemsExtension.create());
        markdownParser = Parser.builder().extensions(extensionList).build();
    }

    @Exposed
    public MarkdownStep(final @Arg("cssClass") @NullableArg Map<String, String> cssClass) {
        if (cssClass == null) {
            this.htmlRenderer = HtmlRenderer.builder().extensions(extensionList).build();
        } else {
            this.htmlRenderer = HtmlRenderer.builder().extensions(extensionList)
                    .attributeProviderFactory(context -> (node, tagName, attributes) -> {
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
