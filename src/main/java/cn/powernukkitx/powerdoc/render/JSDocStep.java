package cn.powernukkitx.powerdoc.render;

import cn.powernukkitx.powerdoc.Document;
import cn.powernukkitx.powerdoc.config.Arg;
import cn.powernukkitx.powerdoc.config.Exposed;
import cn.powernukkitx.powerdoc.config.NullableArg;
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
    private final Map<String, Map<String, String>> langs;
    private String _class;
    private String _function;
    private String _allowEsm;
    private String _type;
    private String _returnType;
    private String _parameters;
    private String _name;
    private String _comment;
    private String _method;
    private String _member;
    private String _belongTo;
    private String _constructor;

    @Exposed
    public JSDocStep(final @Arg("lang") @NullableArg Map<String, Map<String, String>> langs) {
        this.langs = langs;
    }

    @Exposed
    public JSDocStep() {
        this.langs = new HashMap<>(0);
    }

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
        var astProbablyName = docPath.getFileName().toString().replace(".md", "");
        var last_ = astProbablyName.lastIndexOf('_');
        if (last_ != -1 && last_ + 6 == astProbablyName.length()) {
            astProbablyName = astProbablyName.substring(0, last_);
        }
        var astPath = docPath.getParent().resolve(astProbablyName + ".ast.json");
        if (Files.exists(astPath)) {
            try {
                initLanguage(document);
                document.setText(document.getText().replace("%JSDoc%",
                        parseAST(JsonParser.parseString(Files.readString(astPath, StandardCharsets.UTF_8)).getAsJsonArray())
                                .toString()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initLanguage(Document document) {
        var lang = langs.get(document.getVariable("file.language", String.class));
        if (lang == null) lang = new HashMap<>(0);
        this._class = lang.getOrDefault("class", "Class");
        this._function = lang.getOrDefault("function", "Function");
        this._allowEsm = lang.getOrDefault("allow-esm", "ESM-import allowed.");
        this._type = lang.getOrDefault("type", "type");
        this._returnType = lang.getOrDefault("return-type", "return type");
        this._parameters = lang.getOrDefault("parameters", "parameters");
        this._name = lang.getOrDefault("name", "name");
        this._comment = lang.getOrDefault("comment", "comment");
        this._method = lang.getOrDefault("method", "Method");
        this._member = lang.getOrDefault("member", "Member");
        this._belongTo = lang.getOrDefault("belong-to", "Belongs to");
        this._constructor = lang.getOrDefault("constructor", "Constructor");
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
                    functionList.add(new Function(this, name, description, returnTypes, true, params));
                } else if (!"".equals(memberOf) && isMethod) {
                    var clazz = classMap.get(memberOf);
                    if (clazz != null) {
                        clazz.methods.add(new Method(this, name, description, returnTypes, params, clazz));
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
                        clazz.methods.add(new Method(this, name, description, returnTypes, params, clazz));
                    }
                } else {
                    if (!classMap.containsKey(name)) {
                        var clazz = new Class(this, name, description, exported, new ArrayList<>(), new ArrayList<>());
                        classMap.put(name, clazz);
                    }
                }
            } else if ("member".equals(kind)) {
                var clazz = classMap.get(memberOf);
                if (clazz != null) {
                    clazz.fields.add(new Field(this, name, description, parseTypes(node), clazz));
                }
            }
        }
        return new JSProgram(this, functionList, classMap.values().stream().toList());
    }

    private Param parseParam(JsonObject paramNode) {
        var types = parseTypes(paramNode);
        return new Param(this,
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

    record JSProgram(JSDocStep jsDocStep, List<Function> functions, List<Class> classes) {
        @Override
        public String toString() {
            var sb = new StringBuilder();
            if (functions != null && functions.size() > 0) {
                sb.append("## ").append(jsDocStep._function).append("  \n\n");
                sb.append(functions.stream().map(Object::toString).collect(Collectors.joining()));
            }
            if (classes != null && classes.size() > 0) {
                sb.append("## ").append(jsDocStep._class).append("  \n\n");
                sb.append(classes.stream().map(Object::toString).collect(Collectors.joining()));
            }
            return sb.toString();
        }
    }

    record Param(JSDocStep jsDocStep, String name, String description, boolean variable, String... types) {

    }

    record Function(JSDocStep jsDocStep, String name, String description, String[] returnTypes, boolean exported,
                    List<Param> params) {
        @Override
        public String toString() {
            var sb = new StringBuilder("### ");
            sb.append(name).append("  \n");
            if (description != null && !"".equals(description)) {
                sb.append(description).append("  \n");
            }
            if (exported) {
                sb.append(jsDocStep._allowEsm).append("  \n");
            }
            sb.append(jsDocStep._returnType).append(": ").append(collectTypes2String(returnTypes)).append("  \n");
            if (params != null && params.size() > 0) {
                sb.append(jsDocStep._parameters).append(": \n\n");
                sb.append("|").append(jsDocStep._name).append("|").append(jsDocStep._type).append("|").append(jsDocStep._comment).append("|\n").append("|--|--|--|\n");
                for (var each : params) {
                    sb.append("|").append(each.name).append("|").append(collectTypes2String(true, each.types)).append("|").append(each.description).append("|\n");
                }
            }
            sb.append("\n\n");
            return sb.toString();
        }
    }

    record Field(JSDocStep jsDocStep, String name, String description, String[] type, Class parent) {
        @Override
        public String toString() {
            var sb = new StringBuilder("#### ");
            sb.append(name).append("  \n");
            if (description != null && !"".equals(description)) {
                sb.append(description).append("  \n");
            }
            sb.append(jsDocStep._belongTo).append(": `").append(parent.name).append("`  \n");
            sb.append(jsDocStep._type).append(": ").append(collectTypes2String(true, type)).append("\n\n");
            sb.append("\n\n");
            return sb.toString();
        }
    }

    record Method(JSDocStep jsDocStep, String name, String description, String[] returnTypes, List<Param> params,
                  Class parent) {
        @Override
        public String toString() {
            var sb = new StringBuilder("#### ");
            if (parent.name.equals(name)) {
                sb.append(jsDocStep._constructor).append("  \n");
            } else {
                sb.append(name).append("  \n");
            }
            if (description != null && !"".equals(description)) {
                sb.append(description).append("  \n");
            }
            sb.append(jsDocStep._belongTo).append(": `").append(parent.name).append("`  \n");
            sb.append(jsDocStep._returnType).append(": ").append(collectTypes2String(returnTypes)).append("  \n");
            if (params != null && params.size() > 0) {
                sb.append(jsDocStep._parameters).append(": \n\n");
                sb.append("|").append(jsDocStep._name).append("|").append(jsDocStep._type).append("|").append(jsDocStep._comment).append("|\n").append("|--|--|--|\n");
                for (var each : params) {
                    sb.append("|").append(each.name).append("|").append(collectTypes2String(true, each.types)).append("|").append(each.description).append("|\n");
                }
            }
            sb.append("\n\n");
            return sb.toString();
        }
    }

    record Class(JSDocStep jsDocStep, String name, String description, boolean exported, List<Field> fields,
                 List<Method> methods) {
        @Override
        public String toString() {
            var sb = new StringBuilder("### ");
            sb.append(name).append("  \n");
            if (description != null && !"".equals(description)) {
                sb.append(description).append("  \n");
            }
            if (exported) {
                sb.append(jsDocStep._allowEsm).append("  \n");
            }
            if (fields.size() != 0) {
                sb.append("#### ").append(jsDocStep._member).append("  \n");
            }
            for (var each : fields) {
                sb.append(each);
            }
            if (methods.size() != 0) {
                sb.append("#### ").append(jsDocStep._method).append("  \n");
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
