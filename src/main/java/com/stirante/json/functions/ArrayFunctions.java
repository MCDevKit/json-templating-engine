package com.stirante.json.functions;

import com.stirante.json.utils.JsonUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArrayFunctions {

    @JSONFunction
    private static JSONArray asArray(JSONObject obj, String key, String value) {
        return new JSONArray(obj.keySet().stream().map(s -> {
            JSONObject r = new JSONObject();
            r.put(key, s);
            r.put(value, obj.get(s));
            return r;
        }).collect(Collectors.toList()));
    }

    @JSONFunction
    private static JSONArray keys(JSONObject obj) {
        return new JSONArray(new ArrayList<>(obj.keySet()));
    }

    @JSONFunction
    private static JSONArray values(JSONObject obj) {
        return new JSONArray(new ArrayList<>(obj.keySet()));
    }

    @JSONFunction
    private static JSONArray reverse(JSONArray arr) {
        List<?> l = new ArrayList<>(arr.toList());
        Collections.reverse(l);
        return new JSONArray(l);
    }

    @JSONFunction
    private static Boolean contains(JSONArray arr, String value) {
        return arr.toList().stream().anyMatch(o -> o instanceof String && o.equals(value));
    }

    @JSONFunction
    private static Boolean contains(JSONArray arr, Number value) {
        return arr.toList()
                .stream()
                .anyMatch(o -> o instanceof Number && ((Number) o).doubleValue() == value.doubleValue());
    }

    @JSONFunction
    private static JSONArray filter(JSONArray arr, Function<Object, Object> predicate) {
        return new JSONArray(arr.toList()
                .stream()
                .filter(o -> JsonUtils.toBoolean(predicate.apply(o)))
                .collect(Collectors.toList()));
    }

    @JSONFunction
    private static JSONArray map(JSONArray arr, Function<Object, Object> predicate) {
        return new JSONArray(arr.toList().stream().map(predicate).collect(Collectors.toList()));
    }

    @JSONFunction
    private static JSONArray flatMap(JSONArray arr, Function<Object, Object> predicate) {
        return new JSONArray(arr.toList().stream().flatMap(o -> {
            Object apply = predicate.apply(o);
            return apply instanceof JSONArray ? ((JSONArray) apply).toList()
                    .stream() : apply instanceof Collection ? ((Collection<?>) apply).stream() : Stream.of(apply);
        }).collect(Collectors.toList()));
    }

    @JSONFunction
    private static JSONArray flatMap(JSONArray arr) {
        return new JSONArray(arr.toList()
                .stream()
                .flatMap(o -> o instanceof JSONArray ? ((JSONArray) o).toList()
                        .stream() : o instanceof Collection ? ((Collection<?>) o).stream() : Stream.of(o))
                .collect(Collectors.toList()));
    }

}
