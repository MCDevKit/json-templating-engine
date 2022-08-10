package com.glowfischdesignstudio.jsonte;

import com.glowfischdesignstudio.jsonte.exception.JsonTemplatingException;
import com.glowfischdesignstudio.jsonte.functions.JSONLambda;
import com.glowfischdesignstudio.jsonte.utils.ArrayUtils;
import com.glowfischdesignstudio.jsonte.utils.JsonUtils;
import com.glowfischdesignstudio.jsonte.utils.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiFunction;

class FieldVisitor extends JsonTemplateBaseVisitor<Object> {
    private final JSONObject extraScope;
    private final JSONObject fullScope;
    private final Deque<Object> currentScope;
    private final String path;
    private final JsonAction jsonAction;
    private final Deque<Object> scopeStack = new ArrayDeque<>();

    public FieldVisitor(JSONObject extraScope, JSONObject fullScope, Deque<Object> currentScope, String path, JsonAction jsonAction) {
        this.extraScope = extraScope;
        this.fullScope = fullScope;
        this.currentScope = currentScope;
        this.path = path;
        this.jsonAction = jsonAction;
    }

    private void pushScope(JSONObject scope) {
        scopeStack.push(scope);
    }

    private void pushScope(Map<String, Object> scope) {
        scopeStack.push(scope);
    }

    private void pushScope(String name, Object value) {
        pushScope(Map.of(name, value));
    }

    private void popScope() {
        if (!scopeStack.isEmpty()) {
            scopeStack.pop();
        }
    }

    private Object resolveScope(String name) {
        for (Object scope : scopeStack) {
            if (scope instanceof JSONObject && ((JSONObject) scope).has(name)) {
                return ((JSONObject) scope).get(name);
            }
            if (scope instanceof Map && ((Map<?, ?>) scope).containsKey(name)) {
                return ((Map<?, ?>) scope).get(name);
            }
        }
        return null;
    }

    @Override
    public Object visitArray(JsonTemplateParser.ArrayContext ctx) {
        JSONArray result = new JSONArray();
        for (JsonTemplateParser.FieldContext f : ctx.field()) {
            result.put(visit(f));
        }
        return result;
    }

    @Override
    public Object visitObject(JsonTemplateParser.ObjectContext ctx) {
        JSONObject result = new JSONObject();
        for (JsonTemplateParser.Object_fieldContext f : ctx.object_field()) {
            String name = f.ESCAPED_STRING() != null ? StringUtils.unescape(f.ESCAPED_STRING().getText()) : f.name()
                    .getText();
            result.put(name, visit(f));
        }
        return result;
    }

    @Override
    public Object visitIndex(JsonTemplateParser.IndexContext context) {
        if (context.NUMBER() != null) {
            return parseNumber(context.NUMBER().getText());
        }
        else if (context.ESCAPED_STRING() != null) {
            return StringUtils.unescape(context.NUMBER().getText());
        }
        else if (context.field() != null) {
            return visit(context.field());
        }
        return -1;
    }

    @Override
    public Object visitName(JsonTemplateParser.NameContext context) {
        String text = context.getText();
        if (text.equals("this")) {
            return currentScope.peek();
        }
        Object newScope = resolveScope(text);

        if (newScope == null && text.equals("value")) {
            return currentScope.peek();
        }

        Iterator<Object> it = currentScope.iterator();
        while (newScope == null && it.hasNext()) {
            Object scope = it.next();
            if (scope instanceof JSONObject &&
                    ((JSONObject) scope).has(text)) {
                newScope = ((JSONObject) scope).get(text);
            }
        }
        if (newScope == null && extraScope.has(text)) {
            newScope = extraScope.get(text);
        }
        if (newScope == null && fullScope.has(text)) {
            newScope = fullScope.get(text);
        }
        return newScope;
    }

    @Override
    public Object visitField(JsonTemplateParser.FieldContext context) {
        if (context.Null() != null) {
            return null;
        }
        if (context.True() != null) {
            return true;
        }
        if (context.False() != null) {
            return false;
        }
        if (context.Not() != null) {
            return !JsonUtils.toBoolean(visit(context.field(0)));
        }
        if (context.field().size() == 2) {
            // Move AND and OR here to make those operators short-circuiting
            if (context.And() != null) {
                return JsonUtils.toBoolean(visit(context.field(0))) && JsonUtils.toBoolean(visit(context.field(1)));
            }
            else if (context.Or() != null) {
                return JsonUtils.toBoolean(visit(context.field(0))) || JsonUtils.toBoolean(visit(context.field(1)));
            }
            else if (context.Question() != null) {
                return JsonUtils.toBoolean(visit(context.field(0))) ? visit(context.field(1)) : null;
            }
            Object f1 = visit(context.field(0));
            Object f2 = visit(context.field(1));
            Number n1 = JsonUtils.toNumber(f1);
            Number n2 = JsonUtils.toNumber(f2);
            if (context.NullCoalescing() != null) {
                return f1 == null ? f2 : f1;
            }
            else if (context.Add() != null) {
                if (((f1 instanceof Number && f2 instanceof Number) ||
                        (f1 instanceof Boolean && f2 instanceof Boolean)) && n1 != null && n2 != null) {
                    boolean decimal = f1 instanceof Float || f1 instanceof Double || f2 instanceof Float ||
                            f2 instanceof Double;
                    if (decimal) {
                        return n1.doubleValue() + n2.doubleValue();
                    }
                    return n1.intValue() + n2.intValue();
                }
                else if ((f1 instanceof JSONArray || f1 instanceof List) &&
                        (f2 instanceof JSONArray || f2 instanceof List)) {
                    JSONArray a1 = f1 instanceof List ? new JSONArray((List) f1) : (JSONArray) f1;
                    JSONArray a2 = f2 instanceof List ? new JSONArray((List) f2) : (JSONArray) f2;
                    JSONArray a3 = new JSONArray();
                    for (int i = 0; i < a1.length(); i++) {
                        a3.put(a1.get(i));
                    }
                    for (int i = 0; i < a2.length(); i++) {
                        a3.put(a2.get(i));
                    }
                    return a3;
                }
                else if ((f1 instanceof JSONObject || f1 instanceof Map) &&
                        (f2 instanceof JSONObject || f2 instanceof Map)) {
                    JSONObject a1 = f1 instanceof Map ? new JSONObject((Map) f1) : (JSONObject) f1;
                    JSONObject a2 = f2 instanceof Map ? new JSONObject((Map) f2) : (JSONObject) f2;
                    JSONObject a3 = (JSONObject) JsonUtils.copyJson(a1);
                    JsonUtils.merge(a3, a2);
                    return a3;
                }
                else {
                    return f1.toString() + f2.toString();
                }
            }
            else if (context.Equal() != null) {
                if (f1 instanceof Number && f2 instanceof Number) {
                    return ((Number) f1).doubleValue() == ((Number) f2).doubleValue();
                }
                return Objects.equals(f1, f2);
            }
            else if (context.NotEqual() != null) {
                return !Objects.equals(f1, f2);
            }
            else if (n1 != null && n2 != null) {
                if (context.Range() != null) {
                    return ArrayUtils.range(n1.intValue(), n2.intValue());
                }
                if (context.Greater() != null) {
                    return n1.doubleValue() > n2.doubleValue();
                }
                if (context.Less() != null) {
                    return n1.doubleValue() < n2.doubleValue();
                }
                if (context.GreaterOrEqual() != null) {
                    return n1.doubleValue() >= n2.doubleValue();
                }
                if (context.LessOrEqual() != null) {
                    return n1.doubleValue() <= n2.doubleValue();
                }
                boolean decimal = n1 instanceof Float || n1 instanceof Double || n1 instanceof BigDecimal ||
                        n2 instanceof Float ||
                        n2 instanceof Double || n2 instanceof BigDecimal;
                if (decimal) {
                    if (context.Subtract() != null) {
                        return n1.doubleValue() - n2.doubleValue();
                    }
                    if (context.Divide() != null) {
                        return n1.doubleValue() / n2.doubleValue();
                    }
                    if (context.Multiply() != null) {
                        return n1.doubleValue() * n2.doubleValue();
                    }
                }
                else {
                    if (context.Subtract() != null) {
                        return n1.intValue() - n2.intValue();
                    }
                    if (context.Divide() != null) {
                        return n1.intValue() / n2.intValue();
                    }
                    if (context.Multiply() != null) {
                        return n1.intValue() * n2.intValue();
                    }
                }
            }
            else if (context.Greater() != null || context.Less() != null || context.GreaterOrEqual() != null ||
                    context.LessOrEqual() != null) {
                return false;
            }
            else {
                return "NaN";
            }
        }
        else if (context.field().size() == 3) {
            if (context.Question() != null) {
                return JsonUtils.toBoolean(visit(context.field(0))) ? visit(context.field(1)) : visit(context.field(2));
            }
        }
        if (context.LeftParen() != null && context.field().size() == 1 &&
                context.children.indexOf(context.field(0)) == 0) {
            Object lambda = visit(context.field(0));
            Object[] params = context.function_param().stream().map(this::visit).toArray();
            if (lambda instanceof String) {
                JsonTemplateParser.LambdaContext lambdaContext = JsonProcessor.resolveLambdaTree((String) lambda, path);
                Object func = visit(lambdaContext);
                if (func instanceof JSONLambda) {
                    return ((JSONLambda) func).apply(params);
                }
                else {
                    throw new JsonTemplatingException(String.format("Function '%s' not found!", context.field(0)
                            .getText()), path);
                }
            }
            else if (lambda instanceof BiFunction) {
                //noinspection unchecked
                return ((BiFunction<Object[], String, Object>) lambda).apply(params, path);
            }
            else {
                String methodName = context.field(0).name().getText();
                if (!JsonProcessor.FUNCTIONS.containsKey(methodName)) {
                    throw new JsonTemplatingException("Function '" + methodName + "' not found!", path);
                }
                return JsonProcessor.FUNCTIONS.get(methodName).execute(params, path);
            }
        }
        else if (context.LeftParen() != null && context.field().size() == 1 &&
                context.children.indexOf(context.field(0)) != 0) {
            return visit(context.field(0));
        }
        if (context.name() != null && context.field().size() == 1) {
            String text = context.name().getText();
            Object object = visit(context.field(0));
            Object newScope = null;
            if (object instanceof JSONObject && ((JSONObject) object).has(text)) {
                newScope = ((JSONObject) object).get(text);
            }
            // After adding lambdas, we also need to check for maps
            else if (object instanceof Map && ((Map<?, ?>) object).containsKey(text)) {
                newScope = ((Map<?, ?>) object).get(text);
            }
            else {
                if (object instanceof JSONArray || object instanceof List) {
                    if (JsonProcessor.INSTANCE_FUNCTIONS.containsKey(JSONArray.class) &&
                            JsonProcessor.INSTANCE_FUNCTIONS.get(JSONArray.class).containsKey(text)) {
                        if (object instanceof List) {
                            object = new JSONArray((List<?>) object);
                        }
                        Object finalObject = object;
                        return (BiFunction<Object[], String, Object>) (params, path) -> JsonProcessor.INSTANCE_FUNCTIONS.get(JSONArray.class)
                                .get(text)
                                .execute(
                                        ArrayUtils.prepend(finalObject, params), path
                                );
                    }
                    throw new JsonTemplatingException("Trying to access field from an array", path);
                }

                if (object instanceof String) {
                    if (JsonProcessor.INSTANCE_FUNCTIONS.containsKey(String.class) &&
                            JsonProcessor.INSTANCE_FUNCTIONS.get(String.class).containsKey(text)) {
                        Object finalObject = object;
                        return (BiFunction<Object[], String, Object>) (params, path) -> JsonProcessor.INSTANCE_FUNCTIONS.get(String.class)
                                .get(text)
                                .execute(
                                        ArrayUtils.prepend(finalObject, params), path
                                );
                    }
                    throw new JsonTemplatingException("Trying to access field from a string", path);
                }

                if (object instanceof Number) {
                    throw new JsonTemplatingException("Trying to access field from a number", path);
                }
            }

            if (newScope == null) {
                if (jsonAction == JsonAction.PREDICATE || context.Question() != null) {
                    return null;
                }
                throw new JsonTemplatingException("Failed to resolve \"" + context.getText() + "\" in ", path);
            }
            return newScope;
        }
        if (context.name() != null) {
            return visit(context.name());
        }
        if (context.index() != null && context.field().size() == 1) {
            Object i = visit(context.index());
            Object object = visit(context.field(0));
            if (object instanceof JSONArray) {
                JSONArray arr = (JSONArray) object;
                if (!(i instanceof Number)) {
                    if (context.Question() != null) {
                        return null;
                    }
                    throw new JsonTemplatingException("Array index is not a number!", path);
                }
                int index = ((Number) i).intValue();
                if (index >= arr.length() || index < 0) {
                    if (context.Question() != null) {
                        return null;
                    }
                    throw new JsonTemplatingException("Array index out of bounds!", path);
                }
                return arr.toList().get(index);
            }
            // After adding lambdas, we also need to check for lists
            if (object instanceof List) {
                List<?> arr = (List<?>) object;
                if (!(i instanceof Number)) {
                    if (context.Question() != null) {
                        return null;
                    }
                    throw new JsonTemplatingException("Array index is not a number!", path);
                }
                int index = ((Number) i).intValue();
                if (index >= arr.size() || index < 0) {
                    if (context.Question() != null) {
                        return null;
                    }
                    throw new JsonTemplatingException("Array index out of bounds!", path);
                }
                return arr.get(index);
            }
            else if (object instanceof JSONObject) {
                JSONObject obj = (JSONObject) object;
                if (i instanceof Number) {
                    int index = ((Number) i).intValue();
                    if (index >= obj.length() || index < 0) {
                        if (context.Question() != null) {
                            return null;
                        }
                        throw new JsonTemplatingException("Object index out of bounds!", path);
                    }
                    return JsonUtils.getByIndex(obj, index);
                }
                if (i instanceof String) {
                    if (!obj.has((String) i)) {
                        if (context.Question() != null) {
                            return null;
                        }
                        throw new JsonTemplatingException("Object does not have key \"" + i + "\"!", path);
                    }
                    return obj.get((String) i);
                }
                throw new JsonTemplatingException("Object index is not a number or string!", path);
            }
            // After adding lambdas, we also need to check for maps
            else if (object instanceof Map) {
                //noinspection unchecked
                Map<String, ?> obj = (Map<String, ?>) object;
                if (i instanceof Number) {
                    int index = ((Number) i).intValue();
                    if (index >= obj.size() || index < 0) {
                        if (context.Question() != null) {
                            return null;
                        }
                        throw new JsonTemplatingException("Object index out of bounds!", path);
                    }
                    return obj.get(new ArrayList<>(obj.keySet()).get(((Number) i).intValue()));
                }
                if (i instanceof String) {
                    if (!obj.containsKey((String) i)) {
                        if (context.Question() != null) {
                            return null;
                        }
                        throw new JsonTemplatingException("Object does not have key \"" + i + "\"!", path);
                    }
                    return obj.get((String) i);
                }
                throw new JsonTemplatingException("Object index is not a number or string!", path);
            }
        }
        if (context.NUMBER() != null) {
            return parseNumber(context.NUMBER().getText());
        }
        if (context.ESCAPED_STRING() != null) {
            return StringUtils.unescape(context.ESCAPED_STRING().getText());
        }
        if (context.array() != null) {
            return visit(context.array());
        }
        if (context.object() != null) {
            return visit(context.object());
        }
        if (context.Subtract() != null && context.field().size() == 1) {
            return negate(visit(context.field(0)));
        }
        return null;
    }

//    @Override
//    public Object visitFunction(JsonTemplateParser.FunctionContext ctx) {
//        String methodName = ctx.name().getText();
//        if (!JsonProcessor.FUNCTIONS.containsKey(methodName)) {
//            throw new JsonTemplatingException("Function '" + methodName + "' not found!", path);
//        }
//        Object[] params = ctx.function_param().stream().map(this::visit).toArray();
//        return JsonProcessor.FUNCTIONS.get(methodName).execute(params, path);
//    }

    @Override
    public Object visitLambda(JsonTemplateParser.LambdaContext ctx) {
        return (JSONLambda) o -> {
            if (ctx.name().size() > o.length) {
                throw new JsonTemplatingException(
                        String.format("Lambda requires %d parameters, but only %d were supplied!", ctx.name()
                                .size(), o.length), path);
            }
            for (int i = 0; i < ctx.name().size(); i++) {
                pushScope(ctx.name(i).getText(), o[i]);
            }
            Object result = visit(ctx.field());
            for (int i = 0; i < ctx.name().size(); i++) {
                popScope();
            }
            return result;
        };
    }

    private Object parseNumber(String number) {
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return Double.parseDouble(number);
        }
    }

    private Object negate(Object o) {
        if (o instanceof Integer) {
            return -(Integer) o;
        }
        else if (o instanceof Double) {
            return -(Double) o;
        }
        else if (o instanceof Float) {
            return -(Float) o;
        }
        else if (o instanceof Long) {
            return -(Long) o;
        }
        else if (o instanceof BigDecimal) {
            return -((BigDecimal) o).doubleValue();
        }
        else if (o instanceof JSONArray) {
            JSONArray arr = (JSONArray) o;
            for (int i = 0; i < arr.length(); i++) {
                arr.put(i, negate(arr.get(i)));
            }
            return arr;
        }
        else if (o instanceof List) {
            //noinspection unchecked
            List<Object> arr = (List<Object>) o;
            for (int i = 0; i < arr.size(); i++) {
                arr.add(i, negate(arr.get(i)));
            }
            return arr;
        }
        else {
            return "NaN";
        }
    }

}
