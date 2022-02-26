package cn.powernukkitx.powerdoc.render;

import java.util.HashMap;
import java.util.Map;

public interface Step {
    String getName();

    String getId();

    void work(Document document);

    default DocumentStepRecord getStepRecord(Document document) {
        return new DocumentStepRecord(getName(), document);
    }

    Map<String, Class<? extends Step>> registeredStep = new HashMap<>();

    static void registerStep(String id, Class<? extends Step> stepClass) {
        registeredStep.put(id, stepClass);
    }

    static Class<? extends Step> getStepClass(String id) {
        if(registeredStep.size() == 0) {
            initInnerStep();
        }
        return registeredStep.get(id);
    }

    static void initInnerStep() {
        Step.registerStep("html-format", HtmlFormatStep.class);
        Step.registerStep("markdown", MarkdownStep.class);
        Step.registerStep("catalogue", HtmlCatalogueStep.class);
    }
}
