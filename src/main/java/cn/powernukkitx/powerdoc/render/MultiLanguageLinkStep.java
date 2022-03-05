package cn.powernukkitx.powerdoc.render;


import cn.powernukkitx.powerdoc.Document;
import cn.powernukkitx.powerdoc.config.MultiLanguageDocuments;
import cn.powernukkitx.powerdoc.utils.FileUtils;
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
            final var outputPath = document.getVariable("file.outputPath", String.class);
            final var rootRelativePrefix = document.getVariable("file.rootRelativePrefix", String.class);
            final var bookDir = FileUtils.of(document.getBook().getConfig().pages().path());
            final var documentDir = FileUtils.of(document.getSource().toString()).getParentFile();
            var path = rootRelativePrefix;
            var relative = FileUtils.relativeWebPath(documentDir, bookDir);
            path += relative;
            path += ("/" + multiLanguageDoc.path());
            jsonObject.addProperty(multiLanguageDoc.language(), path);
//            final var parentPath = Path.of(outputPath).getParent().toString();
//            for (final var each : multiLanguageDoc.sameGroups()) {
//                jsonObject.addProperty(each.language(), parentPath + "/" + each.path());
//            }
        } else {
            jsonObject.addProperty(document.getVariable("file.language", String.class), "./");
        }
        document.setVariable("json.multiLanguageLinks", gson.toJson(jsonObject));
    }
}
