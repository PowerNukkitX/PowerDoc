package cn.powernukkitx.powerdoc.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MultiLanguageDocuments {
    public final Map<String, List<Document>> language2documentMap;
    public final Map<String, Document> fileName2DocumentMap;

    public MultiLanguageDocuments(JsonArray languageDocumentArray) {
        final var map = new HashMap<String, List<Document>>();
        final var m2 = new HashMap<String, Document>();
        language2documentMap = map;
        fileName2DocumentMap = m2;
        for(var each : languageDocumentArray) {
            if(each instanceof JsonObject object) {
                var sameGroups = new Document[object.size()];
                var sameGroupDocuments = new ArrayList<Document>(object.size());
                for(var entry : object.entrySet()) {
                    final var document = new Document(entry.getValue().getAsString(), entry.getKey(), sameGroups);
                    m2.put(document.path, document);
                    sameGroupDocuments.add(document);
                    if(map.containsKey(document.language)) {
                        map.get(document.language).add(document);
                    }else {
                        final var list = new ArrayList<Document>();
                        list.add(document);
                        map.put(document.language, list);
                    }
                }
                System.arraycopy(sameGroupDocuments.toArray(new Document[0]), 0, sameGroups, 0, sameGroups.length);
            }
        }
    }

    public record Document(String path, String language, Document... sameGroups) {

    }
}
