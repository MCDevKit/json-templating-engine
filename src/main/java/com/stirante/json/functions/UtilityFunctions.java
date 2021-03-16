package com.stirante.json.functions;

import com.stirante.json.JsonProcessor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.stream.Collectors;

public class UtilityFunctions {

    public static void register() {
        JsonProcessor.defineFunction(
                new JsonProcessor.FunctionDefinition("asArray")
                        .implementation(UtilityFunctions::asArray, JSONObject.class, String.class, String.class)
        );
    }

    private static Object asArray(Object[] params) {
        JSONObject obj = (JSONObject) params[0];
        return new JSONArray(obj.keySet().stream().map(s -> {
            JSONObject r = new JSONObject();
            r.put((String) params[1], s);
            r.put((String) params[2], obj.get(s));
            return r;
        }).collect(Collectors.toList()));
    }

}
