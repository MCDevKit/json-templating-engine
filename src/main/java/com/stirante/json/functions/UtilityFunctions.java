package com.stirante.json.functions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.stream.Collectors;

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

}
