package com.stirante.json.functions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class UtilityFunctions {

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
    private static JSONArray mapValues(JSONArray arr, JSONObject map) {
        return new JSONArray(StreamSupport.stream(arr.spliterator(), false)
                .map(o -> map.get(o.toString()))
                .collect(Collectors.toList()));
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
        return arr.toList().stream().anyMatch(o -> o instanceof Number && ((Number) o).doubleValue() == value.doubleValue());
    }

}
