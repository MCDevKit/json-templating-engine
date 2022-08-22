package com.glowfischdesignstudio.jsonte.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class JsonUtils {

    /**
     * Removes all fields with null values.
     * @param obj The object to remove nulls from.
     */
    public static void removeNulls(JSONObject obj) {
        List<String> keys = new ArrayList<>(obj.keySet());
        for (String s : keys) {
            Object o = obj.get(s);
            removeNulls(o, s, obj::remove);
        }
    }

    /**
     * Removes all elements with null values.
     * @param arr The array to remove nulls from.
     */
    public static void removeNulls(JSONArray arr) {
        for (int i = arr.length() - 1; i >= 0; i--) {
            Object o = arr.get(i);
            removeNulls(o, i, arr::remove);
        }
    }

    private static <T> void removeNulls(Object o, T param, Consumer<T> remove) {
        if (o == JSONObject.NULL || (o instanceof String && o.equals("null"))) {
            remove.accept(param);
        }
        else if (o instanceof JSONObject) {
            removeNulls((JSONObject) o);
        }
        else if (o instanceof JSONArray) {
            removeNulls((JSONArray) o);
        }
    }

    /**
     * Merges fields from parent to template.
     * @param template The template object.
     * @param parent The parent object.
     * @return merged object.
     */
    public static JSONObject merge(JSONObject template, JSONObject parent) {
        if (template == parent) {
            throw new IllegalArgumentException("Template and parent cannot be the same!");
        }
        for (String s : parent.keySet()) {
            if (s.startsWith("$") && !s.equals("$comment")) {
                template.put(s.substring(1), parent.get(s));
            }
            else if (template.has(s)) {
                if (template.get(s) == JSONObject.NULL) {
                    template.remove(s);
                }
                else if (parent.get(s) instanceof JSONObject) {
                    if (template.get(s) instanceof JSONArray) {
                        JSONArray arr = new JSONArray();
                        arr.put(parent.get(s));
                        parent.put(s, arr);
                        merge(template.getJSONArray(s), arr);
                    }
                    else {
                        merge(template.getJSONObject(s), parent.getJSONObject(s));
                    }
                }
                else if (parent.get(s) instanceof JSONArray) {
                    if (template.get(s) instanceof JSONArray) {
                        merge(template.getJSONArray(s), parent.getJSONArray(s));
                    }
                    else if (template.get(s) == JSONObject.NULL ||
                            (template.get(s) instanceof String && template.getString(s).equals("null"))) {
                        template.remove(s);
                    }
                    else {
                        JSONArray arr = new JSONArray();
                        arr.put(template.get(s));
                        template.put(s, arr);
                        merge(arr, parent.getJSONArray(s));
                    }
                }
            }
            else if (parent.get(s) instanceof JSONObject) {
                template.put(s, new JSONObject(parent.getJSONObject(s).toString()));
            }
            else {
                template.put(s, parent.get(s));
            }
        }
        template.keySet().stream().filter(s -> s.startsWith("$") && !s.equals("$comment")).collect(Collectors.toList()).forEach(s -> {
            String name = s.substring(1);
            if (template.has(name)) {
                template.remove(name);
            }
            template.put(name, template.get(s));
            template.remove(s);
        });
        return template;
    }

    /**
     * Merges fields from parent to template.
     * @param template The template array.
     * @param parent The parent array.
     * @return merged object.
     */
    public static JSONArray merge(JSONArray template, JSONArray parent) {
        if (template == parent) {
            throw new IllegalArgumentException("Template and parent cannot be the same!");
        }
        for (Object o : parent) {
            if (o instanceof JSONObject) {
                template.put(new JSONObject(o.toString()));
            }
            else if (o instanceof JSONArray) {
                template.put(new JSONArray(o.toString()));
            }
            else {
                template.put(o);
            }
        }
        return template;
    }

    /**
     * Creates a scope for iteration.
     * @param extraScope The extra scope to add.
     * @param arr The array to iterate over.
     * @param index The index of the array.
     * @param name The variable name containing current element.
     * @return The scope.
     */
    public static JSONObject createIterationExtraScope(JSONObject extraScope, JSONArray arr, int index, String name) {
        JSONObject extra = new JSONObject();
        extra.put("index", index);
        extra.put(name, arr.get(index));
        for (String s1 : extraScope.keySet()) {
            if (!extra.has(s1)) {
                extra.put(s1, extraScope.get(s1));
            }
        }
        return extra;
    }

    /**
     * Tries to make a boolean out of an object.
     *
     * Following values will result in false:
     * <ul>
     *     <li>null</li>
     *     <li>0</li>
     *     <li>""</li>
     *     <li>false</li>
     * </ul>
     * @param o The object.
     * @return The boolean.
     */
    public static boolean toBoolean(Object o) {
        return o != null && (o instanceof JSONObject ||
                o instanceof JSONArray ||
                (o instanceof Boolean && (Boolean) o) ||
                (o instanceof Number && ((Number) o).doubleValue() != 0) ||
                (o instanceof String && !((String) o).isEmpty()));
    }

    /**
     * Tries to make a number out of an object.
     * @param o The object.
     * @return The number.
     */
    public static Number toNumber(Object o) {
        if (o instanceof Number) {
            return (Number) o;
        }
        else if (o instanceof Boolean) {
            return (Boolean) o ? 1 : 0;
        }
        else if (o instanceof String) {
            if (((String) o).indexOf('.') != -1) {
                try {
                    return Double.parseDouble(o.toString());
                } catch (NumberFormatException ignored) {
                }
            }
            else {
                try {
                    return Integer.parseInt(o.toString());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    public static Object getByIndex(JSONObject obj, int index) {
        if (index < 0) {
            return null;
        }
        int i = 0;
        for (Object t : obj.keySet()) {
            if (i == index) {
                return t;
            }
            i++;
        }
        return null;
    }

    public static Object copyJson(Object obj) {
        if (obj instanceof JSONObject) {
            return new JSONObject(obj.toString());
        }
        if (obj instanceof JSONArray) {
            return new JSONArray(obj.toString());
        }
        return obj;
    }
}
