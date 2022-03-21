package com.glowfischdesignstudio.jsonte;

import com.glowfischdesignstudio.jsonte.exception.JsonTemplatingException;
import com.glowfischdesignstudio.jsonte.functions.FunctionDefinition;
import com.glowfischdesignstudio.jsonte.functions.JSONFunction;
import com.glowfischdesignstudio.jsonte.functions.JSONInstanceFunction;
import com.glowfischdesignstudio.jsonte.functions.JSONLambda;
import com.glowfischdesignstudio.jsonte.functions.impl.*;
import com.glowfischdesignstudio.jsonte.utils.JsonUtils;
import com.stirante.justpipe.Pipe;
import org.antlr.v4.runtime.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JsonProcessor {

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{[^{}]+}}");
    private static final Pattern ACTION_PATTERN = Pattern.compile("^\\{\\{[^{}]+}}$");

    private static final String[] DANGEROUS_FUNCTIONS =
            {"fileList", "fileListRecurse", "imageWidth", "imageHeight", "getMinecraftInstallDir", "audioDuration", "isDir", "load"};
    public static final Map<String, FunctionDefinition> FUNCTIONS = new HashMap<>();
    public static final Map<Class<?>, Map<String, FunctionDefinition>> INSTANCE_FUNCTIONS = new HashMap<>();
    private static final List<Class<?>> ALLOWED_TYPES = Arrays.asList(
            String.class, Integer.class, Double.class, Float.class, Number.class, Boolean.class, Long.class, JSONArray.class, JSONObject.class, JSONLambda.class, Object.class);

    private static boolean SAFE_MODE = false;

    private static final Map<String, JsonModule> MODULES = new HashMap<>();

    static {
        register(StringFunctions.class);
        register(FileFunctions.class);
        register(ColorFunctions.class);
        register(ImageFunctions.class);
        register(MathFunctions.class);
        register(UtilityFunctions.class);
        register(ArrayFunctions.class);
        register(MinecraftFunctions.class);
        register(AudioFunctions.class);
    }

    public static FunctionDefinition defineFunction(String name) {
        return FUNCTIONS.computeIfAbsent(name, FunctionDefinition::new);
    }

    public static FunctionDefinition defineInstanceFunction(Class<?> instanceClass, String name) {
        return INSTANCE_FUNCTIONS.computeIfAbsent(instanceClass, c -> new HashMap<>())
                .computeIfAbsent(name, FunctionDefinition::new);
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

    public static void processModule(String input) {
        JSONObject root = new JSONObject(input);
        if (!root.has("$template")) {
            throw new JsonTemplatingException("Module does not have a template!");
        }
        JSONObject template = root.getJSONObject("$template");
        JSONObject scope = root.optJSONObject("$scope", new JSONObject());
        if (!root.has("$module")) {
            throw new JsonTemplatingException("Module does not have a name!");
        }
        if (template != null) {
            MODULES.put(root.getString("$module"), new JsonModule(template, scope));
        }
        else {
            throw new JsonTemplatingException("A module must be an object!");
        }
    }

    /**
     * Extends the template with modules defined by the object.
     *
     * @param extend   Modules to extend the template with.
     * @param template Template to extend.
     * @param isCopy   Whether to copy the template or not.
     * @param scope    Scope to use.
     * @param extra    Extra scope to use.
     * @return The extended template.
     */
    private static JSONObject extendTemplate(Object extend, JSONObject template, boolean isCopy, JSONObject scope, Deque<Object> currentScope, JSONObject extra, long deadline) {
        List<String> modules = new ArrayList<>();
        if (extend instanceof JSONArray) {
            List<Object> list = ((JSONArray) extend).toList();
            for (int i = 0, listSize = list.size(); i < listSize; i++) {
                Object o = list.get(i);
                String s = (String) o;
                if (ACTION_PATTERN.matcher(s).matches()) {
                    Object value = resolve(s, extra, scope, currentScope, "$extend[" + i + "]").getValue();
                    if (value instanceof JSONArray) {
                        modules.addAll(((JSONArray) value).toList()
                                .stream()
                                .map(Object::toString)
                                .collect(Collectors.toList()));
                    }
                    else if (value instanceof String) {
                        modules.add((String) value);
                    }
                    else {
                        throw new JsonTemplatingException("Invalid value for $extend[" + i + "]!");
                    }
                }
                else {
                    modules.add(s);
                }
            }
        }
        else if (extend instanceof String) {
            String s = (String) extend;
            if (ACTION_PATTERN.matcher(s).matches()) {
                Object value = resolve(s, extra, scope, currentScope, "$extend").getValue();
                if (value instanceof JSONArray) {
                    modules.addAll(((JSONArray) value).toList()
                            .stream()
                            .map(Object::toString)
                            .collect(Collectors.toList()));
                }
                else if (value instanceof String) {
                    modules.add((String) value);
                }
                else {
                    throw new JsonTemplatingException("Invalid value for $extend!");
                }
            }
            else {
                modules.add(s);
            }
        }
        for (String module : modules) {
            if (!MODULES.containsKey(module)) {
                throw new JsonTemplatingException(String.format("Could not find a module named '%s'!", module));
            }
            JsonModule mod = MODULES.get(module);
            if (mod.getTemplate() == null) {
                throw new JsonTemplatingException(String.format("Module '%s' does not have a template!", module));
            }
            JSONObject moduleScope = (JSONObject) JsonUtils.copyJson(scope);
            JsonUtils.merge(moduleScope, mod.getScope());
            JSONObject parent =
                    (JSONObject) visit(JsonUtils.copyJson(mod.getTemplate()), extra, moduleScope, currentScope, "[Module " + module + "]$template", deadline);
            if (isCopy) {
                JsonUtils.merge(template, parent);
            }
            else {
                template = JsonUtils.merge(new JSONObject(parent.toString()), template);
            }
        }
        return template;
    }

    /**
     * Processes a template.
     *
     * @param name        Name of the template.
     * @param input       Input to process.
     * @param globalScope Global scope to use.
     * @param timeout     Timeout for the processing in milliseconds.
     * @return The map of name to processed stringified JSON.
     * @throws IOException If required files could not be read while processing.
     */
    public static Map<String, String> processJson(String name, String input, JSONObject globalScope, long timeout) throws IOException {
        // Set up the deadline
        long deadline = System.currentTimeMillis() + timeout;
        if (timeout <= 0) {
            deadline = Long.MAX_VALUE;
        }
        // Parse the input
        Map<String, String> result = new HashMap<>();
        JSONObject root;
        try {
            root = new JSONObject(input);
        } catch (JSONException e) {
            throw new JsonTemplatingException("Could not parse the " + name + " template!", e);
        }

        // Define scope
        JSONObject scope = (JSONObject) JsonUtils.copyJson(globalScope);
        if (root.has("$scope")) {
            scope = JsonUtils.merge(root.getJSONObject("$scope"), globalScope);
        }

        boolean isCopy = root.has("$copy");
        boolean isExtend = root.has("$extend");
        boolean hasTemplate = root.has("$template");

        // If none of the options are defined, return unmodified JSON
        if (!hasTemplate && !isCopy && !isExtend) {
            result.put(name, input);
            return result;
        }

        Object template;
        if (isCopy && SAFE_MODE) {
            throw new JsonTemplatingException("Copy operation is disabled");
        }

        // Process multiple files option
        if (root.has("$files")) {
            JSONObject files = root.getJSONObject("$files");
            String fileName = (String) files.get("fileName");
            JSONArray array =
                    (JSONArray) resolve(files.getString("array"), new JSONObject(), scope, new ArrayDeque<>(List.of(scope)), "$files.array").getValue();
            if (array == null) {
                throw new JsonTemplatingException("$files.array is null in " + name);
            }
            for (int i = 0; i < array.length(); i++) {
                checkDeadline(deadline);
                JSONObject extra = new JSONObject();
                extra.put("index", i);
                extra.put("value", array.get(i));
                if (isCopy) {
                    String copyPath =
                            visitStringValue(root.getString("$copy"), extra, scope, new ArrayDeque<>(List.of(array.get(i))),
                                    name + "#/$copy").toString();
                    if (copyPath.endsWith(".templ")) {
                        Map<String, String> map =
                                processJson("copy", Pipe.from(new File(copyPath)).toString(), globalScope, timeout);
                        if (map.values().size() != 1) {
                            throw new JsonTemplatingException("Cannot copy a template, that produces multiple files!");
                        }
                        template = new JSONObject(map.get("copy"));
                    }
                    else {
                        template =
                                new JSONObject(Pipe.from(new File(copyPath)).toString());
                    }
                }
                else {
                    template = root.get("$template");
                }
                if (isExtend) {
                    template = extendTemplate(root.get("$extend"), (JSONObject) template, isCopy, scope, new ArrayDeque<>(List.of(array.get(i))), extra, deadline);
                }
                if (isCopy && hasTemplate) {
                    template = JsonUtils.merge(new JSONObject(root.getJSONObject("$template")
                            .toString()), (JSONObject) template);
                }
                JsonUtils.removeNulls((JSONObject) template);
                String mFileName = (String) visit(fileName, extra, scope, new ArrayDeque<>(List.of(array.get(i))), "$files.fileName", deadline);
                result.put(mFileName, visitFile(JsonUtils.copyJson(template), extra, scope, new ArrayDeque<>(List.of(array.get(i))), deadline));
            }
        }
        else {
            if (isCopy) {
                String copyPath = visitStringValue(root.getString("$copy"), new JSONObject(), scope, new ArrayDeque<>(List.of(new JSONObject())),
                        name + "#/$copy").toString();
                if (copyPath.endsWith(".templ")) {
                    Map<String, String> map =
                            processJson("copy", Pipe.from(new File(String.valueOf(visitStringValue(copyPath, new JSONObject(), scope, new ArrayDeque<>(List.of(new JSONObject())), "$copy"))))
                                    .toString(), globalScope, timeout);
                    if (map.values().size() != 1) {
                        throw new JsonTemplatingException("Cannot copy a template, that produces multiple files!");
                    }
                    template = new JSONObject(map.get("copy"));
                }
                else {
                    template =
                            new JSONObject(Pipe.from(new File(String.valueOf(visitStringValue(copyPath, new JSONObject(), scope, new ArrayDeque<>(List.of(new JSONObject())), "$copy"))))
                                    .toString());
                }
            }
            else {
                template = root.get("$template");
            }
            if (isExtend && template instanceof JSONObject) {
                template = extendTemplate(root.get("$extend"), (JSONObject) template, isCopy, scope, new ArrayDeque<>(List.of(new JSONObject())), new JSONObject(), deadline);
            }
            else if (isExtend) {
                throw new JsonTemplatingException("Cannot extend template that is not an object!");
            }
            if (isCopy && hasTemplate) {
                template = JsonUtils.merge(root.getJSONObject("$template"), (JSONObject) template);
            }
            JsonUtils.removeNulls((JSONObject) template);
            result.put(name, visitFile(JsonUtils.copyJson(template), new JSONObject(), scope, new ArrayDeque<>(List.of(scope)), deadline));
        }
        return result;
    }

    private static String visitFile(Object template, JSONObject extraScope, JSONObject fullScope, Deque<Object> currentScope, long deadline) {
        visit(template, extraScope, fullScope, currentScope, "$template", deadline);
        return template instanceof JSONObject ? ((JSONObject) template).toString(2) : ((JSONArray) template).toString(2);
    }

    public static JsonTemplateParser.LambdaContext resolveLambdaTree(String src, String path) {
        JsonTemplateLexer lexer = new JsonTemplateLexer(CharStreams.fromString(src));
        JsonTemplateParser parser = new JsonTemplateParser(new CommonTokenStream(lexer));
        setErrorHandlers(src, path, lexer);
        setErrorHandlers(src, path, parser);
        return parser.lambda();
    }

    /**
     * Resolves reference
     *
     * @param reference The reference to resolve
     * @param scope     The scope to resolve the reference within
     * @param path      The path to the reference
     * @return The resolved reference or null if the reference could not be resolved
     */
    public static ReferenceResult resolve(String reference, JSONObject scope, String path) {
        return resolve(reference, new JSONObject(), scope, new ArrayDeque<>(List.of(new JSONObject())), path);
    }

    /**
     * Resolves reference
     *
     * @param reference    The reference to resolve
     * @param extraScope   The extra scope to resolve the reference within (like iteration scope)
     * @param fullScope    The scope to resolve the reference within
     * @param thisInstance The current instance of the object
     * @param path         The path to the reference
     * @return The resolved reference or null if the reference could not be resolved
     */
    public static ReferenceResult resolve(String reference, JSONObject extraScope, JSONObject fullScope, Deque<Object> thisInstance, String path) {
        JsonTemplateLexer lexer = new JsonTemplateLexer(CharStreams.fromString(reference));
        JsonTemplateParser parser = new JsonTemplateParser(new CommonTokenStream(lexer));
        setErrorHandlers(reference, path, lexer);
        setErrorHandlers(reference, path, parser);
        JsonTemplateParser.ActionContext action = parser.action();
        return new ActionVisitor(extraScope, fullScope, thisInstance, path).visit(action);
    }

    private static void setErrorHandlers(String name, String path, Recognizer<?, ?> r) {
        r.removeErrorListeners();
        r.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object o, int i, int i1, String s, RecognitionException e) {
                throw new JsonTemplatingException("Syntax error in \"" + name + "\": " + s, path, e);
            }
        });
    }

    private static Object visit(Object element, JSONObject extraScope, JSONObject fullScope, Deque<Object> currentScope, String path, long deadline) {
        if (element instanceof JSONArray) {
            return visitArray((JSONArray) element, extraScope, fullScope, currentScope, path, deadline);
        }
        else if (element instanceof JSONObject) {
            return visitObject((JSONObject) element, extraScope, fullScope, currentScope, path, deadline);
        }
        else {
            return visitValue(element, extraScope, fullScope, currentScope, path, deadline);
        }
    }

    private static Object visitArray(JSONArray arr, JSONObject extraScope, JSONObject fullScope, Deque<Object> currentScope, String path, long deadline) {
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
                                    Object copy = JsonUtils.copyJson(template);
                                    currentScope.push(arr1.get(i1));
                                    copy = visit(copy, extra, fullScope, currentScope,
                                            path + "[" + i + "]", deadline);
                                    nArr.put(copy);
                                    currentScope.pop();
                                }
                            }
                            continue;
                        case VALUE:
                        case LITERAL:
                            nArr.put(visit(arr.get(i), extraScope, fullScope, currentScope,
                                    path + "[" + i + "]", deadline));
                            continue;
                        case PREDICATE:
                            if (JsonUtils.toBoolean(e.getValue())) {
                                Object template;
                                if (obj.get(s) instanceof String && ((String) obj.get(s)).startsWith("{{")) {
                                    template = visitValue(obj.getString(s), extraScope, fullScope, currentScope,
                                            path + "/" + s, deadline);
                                } else {
                                    template = obj.get(s);
                                }
                                Object copy = JsonUtils.copyJson(template);
                                copy = visit(copy, extraScope, fullScope, currentScope,
                                        path + "[" + i + "]" + "/" + s, deadline);
                                nArr.put(copy);
                            }
                            continue;
                    }
                }
            }
            if (arr.get(i) instanceof String && ((String) arr.get(i)).startsWith("{{") &&
                    ((String) arr.get(i)).endsWith("}}")) {
                ReferenceResult e =
                        resolve((String) arr.get(i), extraScope, fullScope, currentScope, path + "[" + i + "]");
                if (e.getAction() == JsonAction.LITERAL && e.getValue() instanceof JSONArray) {
                    nArr.putAll((JSONArray) e.getValue());
                }
                else {
                    nArr.put(e.getValue());
                }
                continue;
            }
            nArr.put(visit(arr.get(i), extraScope, fullScope, currentScope, path + "[" + i + "]", deadline));
        }
        arr.clear();
        arr.putAll(nArr);
        return nArr;
    }

    private static Object visitObject(JSONObject obj, JSONObject extraScope, JSONObject fullScope, Deque<Object> currentScope, String path, long deadline) {
        List<String> toRemove = new ArrayList<>();
        Map<String, Object> toAdd = new LinkedHashMap<>();
        for (String s : obj.keySet()) {
            if (ACTION_PATTERN.matcher(s).matches()) {
                ReferenceResult e = resolve(s, extraScope, fullScope, currentScope, path);
                switch (e.getAction()) {
                    case LITERAL:
                        throw new UnsupportedOperationException("Integer cast is not supported in JSON keys!");
                    case VALUE:
                        Object el = JsonUtils.copyJson(obj.get(s));
                        el = visit(el, extraScope, fullScope, currentScope, path + "/" + s, deadline);
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
                                JSONObject copy = (JSONObject) JsonUtils.copyJson(template);
                                currentScope.push(arr.get(i));
                                copy = (JSONObject) visit(copy, extra, fullScope, currentScope,
                                        path + "/" + s, deadline);
                                for (String s1 : copy.keySet()) {
                                    toAdd.put(s1, copy.get(s1));
                                }
                                currentScope.pop();
                            }
                        }
                        break;
                    case PREDICATE:
                        toRemove.add(s);
                        if (JsonUtils.toBoolean(e.getValue())) {
                            JSONObject template = null;
                            if (obj.get(s) instanceof JSONObject) {
                                template = obj.getJSONObject(s);
                            } else if (obj.get(s) instanceof String && ((String) obj.get(s)).startsWith("{{")) {
                                Object o = visitValue(obj.getString(s), extraScope, fullScope, currentScope,
                                        path + "/" + s, deadline);
                                if (o instanceof JSONObject) {
                                    template = (JSONObject) o;
                                }
                            }
                            if (template == null) {
                                throw new JsonTemplatingException("Predicate value is not an object!", path + "/" + s);
                            }
                            JSONObject copy = (JSONObject) JsonUtils.copyJson(template);
                            copy = (JSONObject) visit(copy, extraScope, fullScope, currentScope,
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
                StringBuffer sb = visitStringValue(s, extraScope, fullScope, currentScope, path);
                Object el = JsonUtils.copyJson(obj.get(s));
                el = visit(el, extraScope, fullScope, currentScope, path + "/" + s, deadline);
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
        return obj;
    }

    private static Object visitValue(Object element, JSONObject extraScope, JSONObject fullScope, Deque<Object> currentScope, String path, long deadline) {
        Matcher m = TEMPLATE_PATTERN.matcher(String.valueOf(element));
        StringBuilder sb = new StringBuilder();
        boolean isNumber = element instanceof Number;
        boolean isLong = element instanceof Long;
        boolean isBoolean = element instanceof Boolean;
        while (m.find()) {
            String toReplace = m.group(0);
            ReferenceResult resolve = resolve(toReplace, extraScope, fullScope, currentScope, path);
            if (resolve.getAction() == JsonAction.LITERAL && resolve.getValue() instanceof Boolean) {
                isBoolean = true;
            }
            if (resolve.getAction() == JsonAction.LITERAL && resolve.getValue() instanceof Number) {
                isNumber = true;
            }
            if (resolve.getAction() == JsonAction.PREDICATE) {
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
                throw new JsonTemplatingException("Expected a number, but got \"" + sb + "\"", path);
            }
        }
        if (isBoolean) {
            return Boolean.parseBoolean(sb.toString());
        }
        return sb.toString();
    }

    private static StringBuffer visitStringValue(String string, JSONObject extraScope, JSONObject fullScope, Deque<Object> currentScope, String path) {
        Matcher m = TEMPLATE_PATTERN.matcher(string);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String toReplace = m.group(0);
            ReferenceResult resolve = resolve(toReplace, extraScope, fullScope, currentScope, path);
            if (resolve.getAction() != JsonAction.VALUE) {
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
                        .addImplementation(objects -> {
                            try {
                                return method.invoke(null, objects);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                e.printStackTrace();
                                return null;
                            }
                        }, types);
            }
            if (method.isAnnotationPresent(JSONInstanceFunction.class)) {
                method.setAccessible(true);
                String name = method.getName();
                Class<?>[] types = method.getParameterTypes();
                if (types.length == 0) {
                    throw new IllegalStateException("Registered instance function doesn't have an instance parameter!");
                }
                Class<?> instanceClass = types[0];
                Class<?> retType = method.getReturnType();
                if (!ALLOWED_TYPES.contains(retType)) {
                    throw new IllegalStateException(
                            "Registered instance function " + name + " returns unsupported type " + retType);
                }
                for (Class<?> type : types) {
                    if (!ALLOWED_TYPES.contains(type)) {
                        throw new IllegalStateException(
                                "Registered instance function " + name + " has unsupported parameter type " + retType);
                    }
                }
                defineInstanceFunction(instanceClass, name)
                        .addImplementation(objects -> {
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

}
