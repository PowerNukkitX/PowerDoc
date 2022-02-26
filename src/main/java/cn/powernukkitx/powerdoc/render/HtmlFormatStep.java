package cn.powernukkitx.powerdoc.render;

import cn.powernukkitx.powerdoc.config.Arg;
import cn.powernukkitx.powerdoc.config.Exposed;
import cn.powernukkitx.powerdoc.config.NullableArg;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HtmlFormatStep implements Step {
    private final String templatePath;
    private String template;
    private final Map<String, String> placeholders;

    @Exposed
    public HtmlFormatStep(@Arg("templatePath") @NullableArg String templatePath,
                          @Arg("templateContent") @NullableArg String template,
                          @Arg("placeholders") Map<String, String> placeholders) {
        this.templatePath = templatePath;
        this.template = template;
        this.placeholders = placeholders;
    }

    @Override
    public String getName() {
        return "HtmlFormatStep";
    }

    @Override
    public String getId() {
        return "html-format";
    }

    @Override
    public void work(Document document) {
        if (template == null && templatePath != null) {
            try {
                template = Files.readString(new File(templatePath).toPath());
            } catch (IOException e) {
                Logger.getLogger("cn.powernukkitx.powerdoc").log(Level.WARNING, "Cannot read " + templatePath + "because: " + e.getMessage());
            }
        }
        if (template == null) {
            throw new StepFailedException("No html template given.");
        }
        var str = template;
        for (final var each : placeholders.entrySet()) {
            var placeholder = "%" + each.getKey() + "%";
            str = str.replace(placeholder, document.getVariable(each.getValue(), String.class, placeholder));
        }
        document.setText(str);
        document.setVariable("ext", "html");
    }
}
