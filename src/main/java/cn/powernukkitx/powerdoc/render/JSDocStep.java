package cn.powernukkitx.powerdoc.render;

import cn.powernukkitx.powerdoc.Document;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static cn.powernukkitx.powerdoc.utils.NullUtils.Ok;

@SuppressWarnings("DuplicatedCode")
public class JSDocStep implements Step {

    @Override
    public String getName() {
        return "JSDocStep";
    }

    @Override
    public String getId() {
        return "js-doc";
    }

    @Override
    public void work(Document document) {
        var docPath = document.getSource();
        var astProbablyName = docPath.getFileName().toString().replace(".md", ".ast.json");
        var last_ = astProbablyName.lastIndexOf('_');
        if (last_ + 5 == astProbablyName.length()) {
            astProbablyName = astProbablyName.substring(0, last_);
        }
        var astPath = docPath.getParent().resolve(astProbablyName);
        if (Files.exists(astPath)) {
            try {
                document.setText(document.getText().replace("%JSDoc%",
                        parseAST(JsonParser.parseString(Files.readString(astPath, StandardCharsets.UTF_8)).getAsJsonArray())
                                .toString()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private JSProgram parseAST(JsonArray astNodes) {
        var functionList = new ArrayList<Function>();
        var classMap = new HashMap<String, Class>();
        for (var each : astNodes) {
            var node = each.getAsJsonObject();
            var kind = Ok(node.get("kind"), JsonElement::getAsString, "");
            var memberOf = Ok(node.get("memberof"), JsonElement::getAsString, "");
            var name = Ok(node.get("name"), JsonElement::getAsString, "");
            var exported = Ok(node.get("meta"), JsonElement::getAsJsonObject, v -> v.get("code"),
                    JsonElement::getAsJsonObject, v -> v.get("name"), JsonElement::getAsString, "").contains("exports.");
            var isMethod = Ok(node.get("meta"), JsonElement::getAsJsonObject, v -> v.get("code"),
                    JsonElement::getAsJsonObject, v -> v.get("type"), JsonElement::getAsString, "").equals("MethodDefinition");
            var description = Ok(node.get("description"), JsonElement::getAsString, "");
            if ("function".equals(kind)) {
                var returnsObj = Ok(node.get("returns"), JsonElement::getAsJsonArray, v -> {
                    if (v.size() > 0) return v.get(0).getAsJsonObject();
                    return null;
                }, node);
                var returnTypes = parseTypes(returnsObj);
                var paramsArr = Ok(node.get("params"), JsonElement::getAsJsonArray, null);
                var params = new ArrayList<Param>();
                if (paramsArr != null) {
                    for (var tmp : paramsArr) {
                        params.add(parseParam(tmp.getAsJsonObject()));
                    }
                }
                if ("".equals(memberOf) && exported) { // 导出的全局函数
                    functionList.add(new Function(name, description, returnTypes, true, params));
                } else if (!"".equals(memberOf) && isMethod) {
                    var clazz = classMap.get(memberOf);
                    if (clazz != null) {
                        clazz.methods.add(new Method(name, description, returnTypes, params, clazz));
                    }
                }
            } else if ("class".equals(kind)) {
                if (isMethod) {
                    var returnsObj = Ok(node.get("returns"), JsonElement::getAsJsonArray, v -> {
                        if (v.size() > 0) return v.get(0).getAsJsonObject();
                        return null;
                    }, node);
                    var returnTypes = parseTypes(returnsObj);
                    var paramsArr = Ok(node.get("params"), JsonElement::getAsJsonArray, null);
                    var params = new ArrayList<Param>();
                    if (paramsArr != null) {
                        for (var tmp : paramsArr) {
                            params.add(parseParam(tmp.getAsJsonObject()));
                        }
                    }
                    var clazz = classMap.get(memberOf);
                    if (clazz != null) {
                        clazz.methods.add(new Method(name, description, returnTypes, params, clazz));
                    }
                } else {
                    if (!classMap.containsKey(name)) {
                        var clazz = new Class(name, description, exported, new ArrayList<>(), new ArrayList<>());
                        classMap.put(name, clazz);
                    }
                }
            } else if ("member".equals(kind)) {
                var clazz = classMap.get(memberOf);
                if (clazz != null) {
                    clazz.fields.add(new Field(name, description, parseTypes(node), clazz));
                }
            }
        }
        return new JSProgram(functionList, classMap.values().stream().toList());
    }

    private Param parseParam(JsonObject paramNode) {
        var types = parseTypes(paramNode);
        return new Param(
                Ok(paramNode.get("name"), JsonElement::getAsString, ""),
                Ok(paramNode.get("description"), JsonElement::getAsString, ""),
                Ok(paramNode.get("variable"), JsonElement::getAsBoolean, false),
                Ok(types, new String[0])
        );
    }

    private String[] parseTypes(JsonObject returnsNode) {
        if (returnsNode == null) {
            return new String[0];
        }
        var typeNames = Ok(returnsNode.get("type"), JsonElement::getAsJsonObject, v -> v.get("names"), JsonElement::getAsJsonArray, null);
        if (typeNames == null) {
            return new String[0];
        }
        String[] types;
        types = new String[typeNames.size()];
        for (int i = 0, len = typeNames.size(); i < len; i++) {
            types[i] = typeNames.get(i).getAsString();
        }
        return types;
    }

    record JSProgram(List<Function> functions, List<Class> classes) {
        @Override
        public String toString() {
            var sb = new StringBuilder();
            if (functions != null && functions.size() > 0) {
                sb.append("## 函数  \n\n");
                sb.append(functions.stream().map(Object::toString).collect(Collectors.joining()));
            }
            if (classes != null && classes.size() > 0) {
                sb.append("## 类  \n\n");
                sb.append(classes.stream().map(Object::toString).collect(Collectors.joining()));
            }
            return sb.toString();
        }
    }

    record Param(String name, String description, boolean variable, String... types) {

    }

    record Function(String name, String description, String[] returnTypes, boolean exported, List<Param> params) {
        @Override
        public String toString() {
            var sb = new StringBuilder("### ");
            sb.append(name).append("  \n");
            if (description != null && !"".equals(description)) {
                sb.append(description).append("  \n");
            }
            if (exported) {
                sb.append("允许通过ESM导入  \n");
            }
            sb.append("返回类型: ").append(collectTypes2String(returnTypes)).append("  \n");
            if (params != null && params.size() > 0) {
                sb.append("参数: \n\n");
                sb.append("|名称|类型|注释|\n").append("|--|--|--|\n");
                for (var each : params) {
                    sb.append("|").append(each.name).append("|").append(collectTypes2String(true, each.types)).append("|").append(each.description).append("|\n");
                }
            }
            sb.append("\n\n");
            return sb.toString();
        }
    }

    record Field(String name, String description, String[] type, Class parent) {
        @Override
        public String toString() {
            var sb = new StringBuilder("#### ");
            sb.append(name).append("  \n");
            if (description != null && !"".equals(description)) {
                sb.append(description).append("  \n");
            }
            sb.append("归属于: `").append(parent.name).append("`  \n");
            sb.append("类型: ").append(collectTypes2String(true, type)).append("\n\n");
            sb.append("\n\n");
            return sb.toString();
        }
    }

    record Method(String name, String description, String[] returnTypes, List<Param> params, Class parent) {
        @Override
        public String toString() {
            var sb = new StringBuilder("#### ");
            if (parent.name.equals(name)) {
                sb.append("构造函数  \n");
            } else {
                sb.append(name).append("  \n");
            }
            if (description != null && !"".equals(description)) {
                sb.append(description).append("  \n");
            }
            sb.append("归属于: `").append(parent.name).append("`  \n");
            sb.append("返回类型: ").append(collectTypes2String(returnTypes)).append("  \n");
            if (params != null && params.size() > 0) {
                sb.append("参数: \n\n");
                sb.append("|名称|类型|注释|\n").append("|--|--|--|\n");
                for (var each : params) {
                    sb.append("|").append(each.name).append("|").append(collectTypes2String(true, each.types)).append("|").append(each.description).append("|\n");
                }
            }
            sb.append("\n\n");
            return sb.toString();
        }
    }

    record Class(String name, String description, boolean exported, List<Field> fields, List<Method> methods) {
        @Override
        public String toString() {
            var sb = new StringBuilder("### ");
            sb.append(name).append("  \n");
            if (description != null && !"".equals(description)) {
                sb.append(description).append("  \n");
            }
            if (exported) {
                sb.append("允许通过ESM导入  \n");
            }
            if (fields.size() != 0) {
                sb.append("#### 成员  \n");
            }
            for (var each : fields) {
                sb.append(each);
            }
            if (methods.size() != 0) {
                sb.append("#### 方法  \n");
            }
            for (var each : methods) {
                sb.append(each);
            }
            return sb.toString();
        }
    }

    private static String collectTypes2String(String... types) {
        return collectTypes2String(false, types);
    }

    private static String collectTypes2String(boolean anyIfNull, String... types) {
        if (types == null || types.length == 0) {
            return anyIfNull ? "`any` " : "`void` ";
        }
        return Arrays.stream(types).map(v -> "`" + v + "` ").collect(Collectors.joining());
    }
}
