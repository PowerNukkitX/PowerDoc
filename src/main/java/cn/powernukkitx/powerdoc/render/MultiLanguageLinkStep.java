package cn.powernukkitx.powerdoc.render;


import cn.powernukkitx.powerdoc.Document;
import cn.powernukkitx.powerdoc.config.MultiLanguageDocuments;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

// TODO: 2022/3/6 修复bug 
public class MultiLanguageLinkStep implements Step {
    private final static Gson gson = new Gson();

    @Override
    public String getName() {
        return "MultiLanguageLinkStep";
    }

    @Override
    public String getId() {
        return "multi-language-links";
    }

    @Override
    public void work(Document document) {
        final var jsonObject = new JsonObject();
        final var multiLanguageDocuments = document.getVariable("file.multiLanguageDocuments", MultiLanguageDocuments.class);
        if (multiLanguageDocuments != null) {
            final var multiLanguageDoc = multiLanguageDocuments.fileName2DocumentMap.get(document.getSource().getFileName().toString());
            if (multiLanguageDoc != null) {
                jsonObject.addProperty(multiLanguageDoc.language(), multiLanguageDoc.path());
                for (final var each : multiLanguageDoc.sameGroups()) {
                    jsonObject.addProperty(each.language(), each.path());
                }
            } else {
                jsonObject.addProperty(document.getVariable("file.language", String.class), "./");
            }
        } else {
            jsonObject.addProperty(document.getVariable("file.language", String.class), "./");
        }
        document.setVariable("json.multiLanguageFiles", gson.toJson(jsonObject));
    }
}
