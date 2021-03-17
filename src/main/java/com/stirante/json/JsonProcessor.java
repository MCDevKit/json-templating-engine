package com.stirante.json;

import com.stirante.json.exception.JsonTemplatingException;
import com.stirante.json.functions.*;
import com.stirante.json.utils.JsonUtils;
import com.stirante.json.utils.StringUtils;
import com.stirante.justpipe.Pipe;
import org.antlr.v4.runtime.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonProcessor {

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{[^{}]+}}");
    private static final Pattern ACTION_PATTERN = Pattern.compile("^\\{\\{[^{}]+}}$");

    private static final String[] DANGEROUS_FUNCTIONS = {"fileList", "fileListRecurse", "imageWidth", "imageHeight"};
    public static final Map<String, FunctionDefinition> FUNCTIONS = new HashMap<>();
    private static final List<Class<?>> ALLOWED_TYPES = Arrays.asList(
            String.class, Integer.class, Double.class, Float.class, Number.class, Boolean.class, Long.class, JSONArray.class, JSONObject.class);

    private static boolean SAFE_MODE = false;

    private static final Map<String, JSONObject> MODULES = new HashMap<>();

    static {
        register(StringFunctions.class);
        register(FileFunctions.class);
        register(ColorFunctions.class);
        register(ImageFunctions.class);
        register(MathFunctions.class);
        register(UtilityFunctions.class);
    }

    public static FunctionDefinition defineFunction(String name) {
        return FUNCTIONS.computeIfAbsent(name, FunctionDefinition::new);
    }

    public static void disableFunction(String name) {
        FUNCTIONS.get(name).disable();
    }

    public static void removeDangerousFunctions() {
        for (String f : DANGEROUS_FUNCTIONS) {
            disableFunction(f);
        }
        SAFE_MODE = true;
    }

    private static Object copyJson(Object obj) {
        Object copy = obj;
        if (obj instanceof JSONObject) {
            JSONObject o = (JSONObject) obj;
            copy = new JSONObject(o, o.keySet().toArray(new String[]{}));
        }
        if (obj instanceof JSONArray) {
            JSONArray o = (JSONArray) obj;
            copy = new JSONArray(o.toList());
        }
        return copy;
    }

    public static void processModule(String input, JSONObject globalScope, long timeout) {
        long deadline = System.currentTimeMillis() + timeout;
        if (timeout <= 0) {
            deadline = Long.MAX_VALUE;
        }
        JSONObject root = new JSONObject(input);
        JSONObject scope = (JSONObject) copyJson(globalScope);
        if (root.has("$scope")) {
            scope = JsonUtils.merge(root.getJSONObject("$scope"), globalScope);
        }
        if (!root.has("$template")) {
            throw new JsonTemplatingException("Module does not have a template!");
        }
        Object template = root.get("$template");
        if (!root.has("$module")) {
            throw new JsonTemplatingException("Module does not have a name!");
        }
        Object module = process(copyJson(template), new JSONObject(), scope, scope, "$template", deadline);
        if (module instanceof JSONObject) {
            MODULES.put(root.getString("$module"), (JSONObject) module);
        }
        else {
            throw new JsonTemplatingException("A module must be an object!");
        }
    }

    private static JSONObject merge(Object extend, JSONObject template, boolean overrideTemplate) {
        List<String> modules = new ArrayList<>();
        if (extend instanceof JSONArray) {
            ((JSONArray) extend).forEach(o -> modules.add(((String) o)));
        }
        else if (extend instanceof String) {
            modules.add((String) extend);
        }
        for (String module : modules) {
            if (!MODULES.containsKey(module)) {
                throw new JsonTemplatingException(String.format("Could not find a module named '%s'!", module));
            }
            JSONObject parent = MODULES.get(module);
            if (overrideTemplate) {
                template = JsonUtils.merge(new JSONObject(parent.toString()), template);
            }
            else {
                JsonUtils.merge(template, parent);
            }
        }
        return template;
    }

    public static Map<String, String> processJson(String name, String input, JSONObject globalScope, long timeout) throws IOException {
        long deadline = System.currentTimeMillis() + timeout;
        if (timeout <= 0) {
            deadline = Long.MAX_VALUE;
        }
        Map<String, String> result = new HashMap<>();
        JSONObject root = new JSONObject(input);
        JSONObject scope = (JSONObject) copyJson(globalScope);
        if (root.has("$scope")) {
            scope = JsonUtils.merge(root.getJSONObject("$scope"), globalScope);
        }
        boolean isCopy = root.has("$copy");
        boolean isExtend = root.has("$extend");
        boolean hasTemplate = root.has("$template");
        if (!hasTemplate && !isCopy && !isExtend) {
            result.put(name, input);
            return result;
        }
        Object template;
        if (isCopy && SAFE_MODE) {
            throw new JsonTemplatingException("Copy operation is disabled");
        }
        if (root.has("$files")) {
            JSONObject files = root.getJSONObject("$files");
            String fileName = (String) files.get("fileName");
            JSONArray array =
                    (JSONArray) resolve(files.getString("array"), new JSONObject(), scope, scope, "$files.array").getValue();
            for (int i = 0; i < array.length(); i++) {
                checkDeadline(deadline);
                JSONObject extra = new JSONObject();
                extra.put("index", i);
                extra.put("value", array.get(i));
                if (isCopy) {
                    template =
                            new JSONObject(Pipe.from(new File(String.valueOf(processTemplateValues(extra, scope, array.get(i), "$copy", root
                                    .getString("$copy"))))).toString());
                }
                else {
                    template = root.get("$template");
                }
                if (isExtend) {
                    template = merge(root.get("$extend"), (JSONObject) template, isCopy);
                }
                if (isCopy && hasTemplate) {
                    template = JsonUtils.merge(new JSONObject(root.getJSONObject("$template")
                            .toString()), (JSONObject) template);
                }
                JsonUtils.removeNulls((JSONObject) template);
                String mFileName = (String) process(fileName, extra, scope, array.get(i), "$files.fileName", deadline);
                result.put(mFileName, processFile(copyJson(template), extra, scope, array.get(i), deadline));
            }
        }
        else {
            if (isCopy) {
                template =
                        new JSONObject(Pipe.from(new File(String.valueOf(processTemplateValues(new JSONObject(), scope, new JSONObject(), "$copy", root
                                .getString("$copy"))))).toString());
            }
            else {
                template = root.get("$template");
            }
            if (isExtend && template instanceof JSONObject) {
                template = merge(root.get("$extend"), (JSONObject) template, isCopy);
            }
            else if (isExtend) {
                throw new JsonTemplatingException("Cannot extend template that is not an object!");
            }
            if (isCopy && hasTemplate) {
                template = JsonUtils.merge(root.getJSONObject("$template"), (JSONObject) template);
            }
            JsonUtils.removeNulls((JSONObject) template);
            result.put(name, processFile(copyJson(template), new JSONObject(), scope, scope, deadline));
        }
        return result;
    }

    private static String processFile(Object template, JSONObject extraScope, JSONObject fullScope, Object currentScope, long deadline) {
        process(template, extraScope, fullScope, currentScope, "$template", deadline);
        return template instanceof JSONObject ? ((JSONObject) template).toString(2) : ((JSONArray) template).toString(2);
    }

    public static ReferenceResult resolve(String reference, JSONObject scope) {
        return resolve(reference, new JSONObject(), scope, null);
    }

    public static ReferenceResult resolve(String reference, JSONObject extraScope, JSONObject scope, Object currentScope) {
        return resolve(reference, extraScope, scope, currentScope, "");
    }

    private static ReferenceResult resolve(String name, JSONObject extraScope, JSONObject fullScope, Object currentScope, String path) {
        JsonTemplateLexer lexer = new JsonTemplateLexer(CharStreams.fromString(name));
        JsonTemplateParser parser = new JsonTemplateParser(new CommonTokenStream(lexer));
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object o, int i, int i1, String s, RecognitionException e) {
                throw new JsonTemplatingException("Syntax error in \"" + name + "\"", path);
            }
        });
        JsonTemplateParser.ActionContext action = parser.action();
        return new ActionVisitor(extraScope, fullScope, currentScope, path).visit(action);
    }

    private static Object process(Object element, JSONObject extraScope, JSONObject fullScope, Object currentScope, String path, long deadline) {
        if (element instanceof JSONArray) {
            JSONArray arr = (JSONArray) element;
            JSONArray nArr = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                checkDeadline(deadline);
                if (arr.get(i) instanceof JSONObject &&
                        arr.getJSONObject(i).keySet().stream().filter(s -> !s.startsWith("$comment")).count() == 1) {
                    JSONObject obj = arr.getJSONObject(i);
                    String s = obj.keySet().stream().filter(s1 -> !s1.startsWith("$comment")).findFirst().orElse("");
                    if (s.startsWith("{{") && s.endsWith("}}")) {
                        ReferenceResult e = resolve(s, extraScope, fullScope, currentScope, path + "[" + i + "]");
                        switch (e.getAction()) {
                            case ITERATION:
                                if (e.getValue() instanceof JSONArray) {
                                    Object template = obj.get(s);
                                    JSONArray arr1 = (JSONArray) e.getValue();
                                    for (int i1 = 0; i1 < arr1.length(); i1++) {
                                        checkDeadline(deadline);
                                        JSONObject extra =
                                                JsonUtils.createIterationExtraScope(extraScope, arr1, i1, e.getName());
                                        Object copy = copyJson(template);
                                        copy = process(copy, extra, fullScope, arr1.get(i1),
                                                path + "[" + i + "]", deadline);
                                        nArr.put(copy);
                                    }
                                }
                                continue;
                            case VALUE:
                            case AS_INT:
                                nArr.put(process(arr.get(i), extraScope, fullScope, currentScope,
                                        path + "[" + i + "]", deadline));
                                continue;
                            case PREDICATE:
                                if (JsonUtils.toBoolean(e.getValue())) {
                                    Object template = obj.get(s);
                                    Object copy = copyJson(template);
                                    copy = process(copy, extraScope, fullScope, currentScope,
                                            path + "[" + i + "]" + "/" + s, deadline);
                                    nArr.put(copy);
                                }
                                continue;
                        }
                    }
                }
                nArr.put(process(arr.get(i), extraScope, fullScope, currentScope, path + "[" + i + "]", deadline));
            }
            arr.clear();
            arr.putAll(nArr);
            return nArr;
        }
        else if (element instanceof JSONObject) {
            JSONObject obj = (JSONObject) element;
            List<String> toRemove = new ArrayList<>();
            Map<String, Object> toAdd = new HashMap<>();
            for (String s : obj.keySet()) {
                if (ACTION_PATTERN.matcher(s).matches()) {
                    ReferenceResult e = resolve(s, extraScope, fullScope, currentScope, path);
                    switch (e.getAction()) {
                        case AS_INT:
                            throw new UnsupportedOperationException("Integer cast is not supported in JSON keys!");
                        case VALUE:
                            Object el = copyJson(obj.get(s));
                            el = process(el, extraScope, fullScope, currentScope, path + "/" + s, deadline);
                            toRemove.add(s);
                            toAdd.put(String.valueOf(e.getValue()), el);
                            break;
                        case ITERATION:
                            if (e.getValue() instanceof JSONArray) {
                                toRemove.add(s);
                                JSONObject template = obj.getJSONObject(s);
                                JSONArray arr = (JSONArray) e.getValue();
                                for (int i = 0; i < arr.length(); i++) {
                                    checkDeadline(deadline);
                                    JSONObject extra =
                                            JsonUtils.createIterationExtraScope(extraScope, arr, i, e.getName());
                                    JSONObject copy = (JSONObject) copyJson(template);
                                    copy = (JSONObject) process(copy, extra, fullScope, arr.get(i),
                                            path + "/" + s, deadline);
                                    for (String s1 : copy.keySet()) {
                                        toAdd.put(s1, copy.get(s1));
                                    }
                                }
                            }
                            break;
                        case PREDICATE:
                            toRemove.add(s);
                            if (JsonUtils.toBoolean(e.getValue())) {
                                JSONObject template = obj.getJSONObject(s);
                                JSONObject copy = (JSONObject) copyJson(template);
                                copy = (JSONObject) process(copy, extraScope, fullScope, currentScope,
                                        path + "/" + s, deadline);
                                for (String s1 : copy.keySet()) {
                                    toAdd.put(s1, copy.get(s1));
                                }
                            }
                            break;
                    }
                }
                else if (s.startsWith("$comment")) {
                    toRemove.add(s);
                }
                else {
                    StringBuffer sb = processTemplateValues(extraScope, fullScope, currentScope, path, s);
                    Object el = copyJson(obj.get(s));
                    el = process(el, extraScope, fullScope, currentScope, path + "/" + s, deadline);
                    if (!s.equals(sb.toString()) || !el.toString().equals(obj.get(s).toString())) {
                        toRemove.add(s);
                        toAdd.put(sb.toString(), el);
                    }
                }
            }
            for (String s : toRemove) {
                obj.remove(s);
            }
            for (String s : toAdd.keySet()) {
                obj.put(s, toAdd.get(s));
            }
        }
        else {
            Matcher m = TEMPLATE_PATTERN.matcher(String.valueOf(element));
            StringBuffer sb = new StringBuffer();
            boolean isNumber = element instanceof Number;
            boolean isLong = element instanceof Long;
            boolean isBoolean = element instanceof Boolean;
            while (m.find()) {
                String toReplace = m.group(0);
                ReferenceResult resolve = resolve(toReplace, extraScope, fullScope, currentScope, path);
                if (resolve.getAction() == Action.AS_INT) {
                    isNumber = true;
                }
                if (resolve.getAction() == Action.PREDICATE) {
                    isBoolean = true;
                }
                if (resolve.getValue() instanceof JSONObject || resolve.getValue() instanceof JSONArray) {
                    return resolve.getValue();
                }
                m.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(resolve.getValue())));
            }
            m.appendTail(sb);
            if (isNumber) {
                try {
                    return isLong ? Long.parseLong(sb.toString()) : Float.parseFloat(sb.toString());
                } catch (NumberFormatException e) {
                    throw new JsonTemplatingException("Expected a number, but got \"" + sb.toString() + "\"", path);
                }
            }
            if (isBoolean) {
                return Boolean.parseBoolean(sb.toString());
            }
            return sb.toString();
        }
        return element;
    }

    private static StringBuffer processTemplateValues(JSONObject extraScope, JSONObject fullScope, Object currentScope, String path, String s) {
        Matcher m = TEMPLATE_PATTERN.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String toReplace = m.group(0);
            ReferenceResult resolve = resolve(toReplace, extraScope, fullScope, currentScope, path);
            if (resolve.getAction() != Action.VALUE) {
                throw new UnsupportedOperationException("Cannot execute action here!");
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(resolve
                    .getValue())));
        }
        m.appendTail(sb);
        return sb;
    }

    private static void checkDeadline(long deadline) {
        if (System.currentTimeMillis() > deadline) {
            throw new RuntimeException("JSON generation time has been limited.");
        }
    }

    public static void register(Class<?> cls) {
        for (Method method : cls.getDeclaredMethods()) {
            if (method.isAnnotationPresent(JSONFunction.class)) {
                method.setAccessible(true);
                String name = method.getName();
                Class<?>[] types = method.getParameterTypes();
                Class<?> retType = method.getReturnType();
                if (!ALLOWED_TYPES.contains(retType)) {
                    throw new IllegalStateException(
                            "Registered function " + name + " returns unsupported type " + retType);
                }
                for (Class<?> type : types) {
                    if (!ALLOWED_TYPES.contains(type)) {
                        throw new IllegalStateException(
                                "Registered function " + name + " has unsupported parameter type " + retType);
                    }
                }
                defineFunction(name)
                        .implementation(objects -> {
                            try {
                                return method.invoke(null, objects);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                e.printStackTrace();
                                return null;
                            }
                        }, types);
            }
        }
    }

    public enum Action {
        VALUE,
        ITERATION,
        AS_INT,
        PREDICATE
    }

    public static class FunctionDefinition {
        private final List<Class<?>[]> types = new ArrayList<>();
        private final List<Function<Object[], Object>> implementations = new ArrayList<>();
        private final String name;

        public FunctionDefinition(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Object execute(Object[] params, String path) {
            for (int i = 0, typesSize = types.size(); i < typesSize; i++) {
                Class<?>[] type = types.get(i);
                if (params.length == type.length) {
                    paramCheck(params, type, path);
                    try {
                        return implementations.get(i).apply(params);
                    } catch (JsonTemplatingException ex) {
                        throw ex.withPath(path);
                    }
                }
            }
            throw new JsonTemplatingException(String.format("Incorrect number of parameters passed to function '%s'!", name), path);
        }

        private void paramCheck(Object[] params, Class<?>[] types, String path) {
            for (int i = 0; i < params.length; i++) {
                Object param = params[i];
                if (!types[i].isInstance(param)) {
                    if (param == null) {
                        if (types[i] == JSONArray.class) {
                            params[i] = new JSONArray();
                            return;
                        }
                        if (types[i] == Integer.class) {
                            params[i] = 0;
                            return;
                        }
                        if (types[i] == JSONObject.class) {
                            params[i] = new JSONObject();
                            return;
                        }
                        if (types[i] == String.class) {
                            params[i] = "";
                            return;
                        }
                    }
                    throw new JsonTemplatingException(String.format("Function '%s' expected %s as %s parameter, but got %s", name, types[i]
                                    .getTypeName(), StringUtils.getFormatterNumber(i + 1),
                            param == null ? "null" : param.getClass()), path);
                }
            }
        }

        public FunctionDefinition implementation(Function<Object[], Object> implementation, Class<?>... types) {
            implementations.add(implementation);
            this.types.add(types);
            return this;
        }

        public void disable() {
            Function<Object[], Object> disabled = objects -> {
                throw new JsonTemplatingException("This function has been disabled");
            };
            for (int i = 0; i < implementations.size(); i++) {
                implementations.set(i, disabled);
            }
        }

    }

}
