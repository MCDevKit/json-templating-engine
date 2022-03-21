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

class ReferenceVisitor extends JsonTemplateBaseVisitor<Object> {
    private final JSONObject extraScope;
    private final JSONObject fullScope;
    private final Deque<Object> currentScope;
    private final String path;
    private final JsonAction jsonAction;
    private final Deque<JSONObject> scopeStack = new ArrayDeque<>();

    public ReferenceVisitor(JSONObject extraScope, JSONObject fullScope, Deque<Object> currentScope, String path, JsonAction jsonAction) {
        this.extraScope = extraScope;
        this.fullScope = fullScope;
        this.currentScope = currentScope;
        this.path = path;
        this.jsonAction = jsonAction;
    }

    private void pushScope(JSONObject scope) {
        scopeStack.push(scope);
    }

    private void pushScope(String name, Object value) {
        pushScope(new JSONObject(Map.of(name, value)));
    }

    private void popScope() {
        if (!scopeStack.isEmpty()) {
            scopeStack.pop();
        }
    }

    private Object resolveScope(String name) {
        for (JSONObject scope : scopeStack) {
            if (scope.has(name)) {
                return scope.get(name);
            }
        }
        return null;
    }

    @Override
    public Object visitArray(JsonTemplateParser.ArrayContext ctx) {
        JSONArray result = new JSONArray();
        for (JsonTemplateParser.ReferenceContext f : ctx.reference()) {
            result.put(visit(f));
        }
        return result;
    }

    @Override
    public Object visitReference(JsonTemplateParser.ReferenceContext ctx) {
        if (ctx.field() != null) {
            return visit(ctx.field());
        }
        if (ctx.reference().size() == 1) {
            Object f1 = visit(ctx.reference(0));
            if (ctx.Not() != null) {
                return !JsonUtils.toBoolean(f1);
            }
            return f1;
        }
        else if (ctx.reference().size() == 2) {
            // Move AND and OR here to make those operators short-circuiting
            if (ctx.And() != null) {
                return JsonUtils.toBoolean(visit(ctx.reference(0))) && JsonUtils.toBoolean(visit(ctx.reference(1)));
            }
            else if (ctx.Or() != null) {
                return JsonUtils.toBoolean(visit(ctx.reference(0))) || JsonUtils.toBoolean(visit(ctx.reference(1)));
            } else if (ctx.Predicate() != null) {
                return JsonUtils.toBoolean(visit(ctx.reference(0))) ? visit(ctx.reference(1)) : null;
            }
            Object f1 = visit(ctx.reference(0));
            Object f2 = visit(ctx.reference(1));
            Number n1 = JsonUtils.toNumber(f1);
            Number n2 = JsonUtils.toNumber(f2);
            if (ctx.Equal() != null) {
                if (f1 instanceof Number && f2 instanceof Number) {
                    return ((Number) f1).doubleValue() == ((Number) f2).doubleValue();
                }
                return Objects.equals(f1, f2);
            }
            else if (ctx.NotEqual() != null) {
                return !Objects.equals(f1, f2);
            }
            else {
                if (n1 != null && n2 != null) {
                    if (ctx.Greater() != null) {
                        return n1.doubleValue() > n2.doubleValue();
                    }
                    if (ctx.Less() != null) {
                        return n1.doubleValue() < n2.doubleValue();
                    }
                    if (ctx.GreaterOrEqual() != null) {
                        return n1.doubleValue() >= n2.doubleValue();
                    }
                    if (ctx.LessOrEqual() != null) {
                        return n1.doubleValue() <= n2.doubleValue();
                    }
                }
                else if (ctx.Greater() != null || ctx.Less() != null || ctx.GreaterOrEqual() != null ||
                        ctx.LessOrEqual() != null) {
                    return false;
                }
                else {
                    return "NaN";
                }
            }
        } else if (ctx.reference().size() == 3) {
            if (ctx.Predicate() != null) {
                return JsonUtils.toBoolean(visit(ctx.reference(0))) ? visit(ctx.reference(1)) : visit(ctx.reference(2));
            }
        }
        return null;
    }

    @Override
    public Object visitIndex(JsonTemplateParser.IndexContext context) {
        if (context.NUMBER() != null) {
            return parseNumber(context.NUMBER().getText());
        }
        else if (context.ESCAPED_STRING() != null) {
            return StringUtils.unescape(context.NUMBER().getText());
        }
        else if (context.reference() != null) {
            return visit(context.reference());
        }
        return -1;
    }

    @Override
    public Object visitName(JsonTemplateParser.NameContext context) {
        String text = context.getText();
        if (text.equals("this")) {
            return currentScope.peek();
        }
        if (text.equals("value")) {
            return currentScope.peek();
        }
        Object newScope = resolveScope(text);

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
        if (context.field().size() == 2) {
            Object f1 = visit(context.field(0));
            Object f2 = visit(context.field(1));
            Number n1 = JsonUtils.toNumber(f1);
            Number n2 = JsonUtils.toNumber(f2);
            if (context.Add() != null) {
                if (f1 instanceof Number && f2 instanceof Number) {
                    boolean decimal = f1 instanceof Float || f1 instanceof Double || f2 instanceof Float ||
                            f2 instanceof Double;
                    if (decimal) {
                        return ((Number) f1).doubleValue() + ((Number) f2).doubleValue();
                    }
                    return ((Number) f1).intValue() + ((Number) f2).intValue();
                }
                else {
                    return f1.toString() + f2.toString();
                }
            } else if (n1 != null && n2 != null) {
                if (context.Range() != null) {
                    JSONArray arr = new JSONArray();
                    int from = n1.intValue();
                    int to = n2.intValue();
                    if (from > to) {
                        return arr;
                    }
                    for (int i = from; i <= to; i++) {
                        arr.put(i);
                    }
                    return arr;
                }
                boolean decimal = n1 instanceof Float || n1 instanceof Double || n1 instanceof BigDecimal || n2 instanceof Float ||
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
        }
        if (context.LeftParen() != null && context.field().size() == 1 && context.children.indexOf(context.field(0)) == 0) {
            Object lambda = visit(context.field(0));
            Object[] params = context.function_param().stream().map(this::visit).toArray();
            if (lambda instanceof String) {
                JsonTemplateParser.LambdaContext lambdaContext = JsonProcessor.resolveLambdaTree((String) lambda, path);
                Object func = visit(lambdaContext);
                if (func instanceof JSONLambda) {
                    return ((JSONLambda) func).apply(params);
                }
                else {
                    throw new JsonTemplatingException("Function '" + context.field(0).getText() + "' not found!", path);
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
        } else if (context.LeftParen() != null && context.field().size() == 1 && context.children.indexOf(context.field(0)) != 0) {
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
                    if (JsonProcessor.INSTANCE_FUNCTIONS.containsKey(JSONArray.class) && JsonProcessor.INSTANCE_FUNCTIONS.get(JSONArray.class).containsKey(text)) {
                        if (object instanceof List) {
                            object = new JSONArray((List<?>)object);
                        }
                        Object finalObject = object;
                        return (BiFunction<Object[], String, Object>) (params, path) -> JsonProcessor.INSTANCE_FUNCTIONS.get(JSONArray.class).get(text).execute(
                                ArrayUtils.prepend(finalObject, params), path
                        );
                    }
                    throw new JsonTemplatingException("Trying to access field from an array", path);
                }

                if (object instanceof String) {
                    if (JsonProcessor.INSTANCE_FUNCTIONS.containsKey(String.class) && JsonProcessor.INSTANCE_FUNCTIONS.get(String.class).containsKey(text)) {
                        Object finalObject = object;
                        return (BiFunction<Object[], String, Object>) (params, path) -> JsonProcessor.INSTANCE_FUNCTIONS.get(String.class).get(text).execute(
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
                if (jsonAction == JsonAction.PREDICATE) {
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
                    throw new JsonTemplatingException("Array index is not a number!", path);
                }
                int index = ((Number) i).intValue();
                if (index >= arr.length() || index < 0) {
                    throw new JsonTemplatingException("Array index out of bounds!", path);
                }
                return arr.toList().get(index);
            }
            // After adding lambdas, we also need to check for lists
            if (object instanceof List) {
                List<?> arr = (List<?>) object;
                if (!(i instanceof Number)) {
                    throw new JsonTemplatingException("Array index is not a number!", path);
                }
                int index = ((Number) i).intValue();
                if (index >= arr.size() || index < 0) {
                    throw new JsonTemplatingException("Array index out of bounds!", path);
                }
                return arr.get(index);
            }
            else if (object instanceof JSONObject) {
                JSONObject obj = (JSONObject) object;
                if (i instanceof Number) {
                    return JsonUtils.getByIndex(obj, ((Number) i).intValue());
                }
                if (i instanceof String && obj.has((String) i)) {
                    return obj.get((String) i);
                }
            }
            // After adding lambdas, we also need to check for maps
            else if (object instanceof Map) {
                //noinspection unchecked
                Map<String, ?> obj = (Map<String, ?>) object;
                if (i instanceof Number) {
                    return obj.get(new ArrayList<>(obj.keySet()).get(((Number) i).intValue()));
                }
                if (i instanceof String && obj.containsKey((String) i)) {
                    return obj.get((String) i);
                }
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
                        "Lambda requires " + ctx.name().size() + " parameters, but only " + o.length +
                                " were supplied!", path);
            }
            for (int i = 0; i < ctx.name().size(); i++) {
                pushScope(ctx.name(i).getText(), o[i]);
            }
            Object result = visit(ctx.reference());
            popScope();
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
