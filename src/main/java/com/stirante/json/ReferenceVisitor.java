package com.stirante.json;

import com.stirante.json.exception.JsonTemplatingException;
import com.stirante.json.utils.JsonUtils;
import com.stirante.json.utils.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

class ReferenceVisitor extends JsonTemplateBaseVisitor<Object> {
    private final JSONObject extraScope;
    private final JSONObject fullScope;
    private final Object currentScope;
    private final String path;
    private final JsonAction jsonAction;
    private final Deque<JSONObject> scopeStack = new ArrayDeque<>();

    public ReferenceVisitor(JSONObject extraScope, JSONObject fullScope, Object currentScope, String path, JsonAction jsonAction) {
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
        for (JsonTemplateParser.FieldContext f : ctx.field()) {
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
            else if (ctx.Subtract() != null) {
                return negate(f1);
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
            }
            Object f1 = visit(ctx.reference(0));
            Object f2 = visit(ctx.reference(1));
            if (ctx.Add() != null) {
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
            }
            else if (ctx.Equal() != null) {
                if (f1 instanceof Number && f2 instanceof Number) {
                    return ((Number) f1).doubleValue() == ((Number) f2).doubleValue();
                }
                return Objects.equals(f1, f2);
            }
            else if (ctx.NotEqual() != null) {
                return !Objects.equals(f1, f2);
            }
            else if (ctx.Range() != null) {
                JSONArray arr = new JSONArray();
                int from = Integer.parseInt(String.valueOf(visit(ctx.reference(0))));
                int to = Integer.parseInt(String.valueOf(visit(ctx.reference(1))));
                if (from > to) {
                    return arr;
                }
                for (int i = from; i <= to; i++) {
                    arr.put(i);
                }
                return arr;
            }
            else {
                if (f1 instanceof Number && f2 instanceof Number) {
                    if (ctx.Greater() != null) {
                        return ((Number) f1).doubleValue() > ((Number) f2).doubleValue();
                    }
                    if (ctx.Less() != null) {
                        return ((Number) f1).doubleValue() < ((Number) f2).doubleValue();
                    }
                    if (ctx.GreaterOrEqual() != null) {
                        return ((Number) f1).doubleValue() >= ((Number) f2).doubleValue();
                    }
                    if (ctx.LessOrEqual() != null) {
                        return ((Number) f1).doubleValue() <= ((Number) f2).doubleValue();
                    }
                    boolean decimal = f1 instanceof Float || f1 instanceof Double || f2 instanceof Float ||
                            f2 instanceof Double;
                    if (decimal) {
                        if (ctx.Subtract() != null) {
                            return ((Number) f1).doubleValue() - ((Number) f2).doubleValue();
                        }
                        if (ctx.Divide() != null) {
                            return ((Number) f1).doubleValue() / ((Number) f2).doubleValue();
                        }
                        if (ctx.Multiply() != null) {
                            return ((Number) f1).doubleValue() * ((Number) f2).doubleValue();
                        }
                    }
                    else {
                        if (ctx.Subtract() != null) {
                            return ((Number) f1).intValue() - ((Number) f2).intValue();
                        }
                        if (ctx.Divide() != null) {
                            return ((Number) f1).intValue() / ((Number) f2).intValue();
                        }
                        if (ctx.Multiply() != null) {
                            return ((Number) f1).intValue() * ((Number) f2).intValue();
                        }
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
            return currentScope;
        }
        if (text.equals("value")) {
            return currentScope;
        }
        Object newScope = resolveScope(text);
        if (newScope == null && currentScope instanceof JSONObject &&
                ((JSONObject) currentScope).has(text)) {
            newScope = ((JSONObject) currentScope).get(text);
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
        if (context.function() != null) {
            return visit(context.function());
        }
        if (context.name() != null && context.field() != null) {
            String text = context.name().getText();
            Object object = visit(context.field());
            Object newScope = null;
            if (object instanceof JSONObject && ((JSONObject) object).has(text)) {
                newScope = ((JSONObject) object).get(text);
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
        if (context.index() != null && context.field() != null) {
            Object i = visit(context.index());
            Object object = visit(context.field());
            if (object instanceof JSONArray) {
                JSONArray arr = (JSONArray) object;
                if (!(i instanceof Number)) {
                    throw new JsonTemplatingException("Array index is not a number!", path);
                }
                int index = ((Number) i).intValue();
                if (index >= arr.length() || index < 0) throw new JsonTemplatingException("Array index out of bounds!");
                return arr.toList().get(index);
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
        return null;
    }

    @Override
    public Object visitFunction(JsonTemplateParser.FunctionContext ctx) {
        String methodName = ctx.name().getText();
        if (!JsonProcessor.FUNCTIONS.containsKey(methodName)) {
            throw new JsonTemplatingException("Function '" + methodName + "' not found!", path);
        }
        Object[] params = ctx.function_param().stream().map(this::visit).toArray();
        return JsonProcessor.FUNCTIONS.get(methodName).execute(params, path);
    }

    @Override
    public Object visitLambda(JsonTemplateParser.LambdaContext ctx) {
        return (Function<Object, Object>) o -> {
            pushScope(ctx.name().getText(), o);
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
        else {
            return "NaN";
        }
    }

}
