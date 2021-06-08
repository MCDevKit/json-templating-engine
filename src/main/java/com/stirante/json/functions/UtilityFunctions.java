package com.stirante.json.functions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class UtilityFunctions {

    @Deprecated
    @JSONFunction
    private static JSONArray mapValues(JSONArray arr, JSONObject map) {
        return new JSONArray(StreamSupport.stream(arr.spliterator(), false)
                .map(o -> map.get(o.toString()))
                .collect(Collectors.toList()));
    }

}
