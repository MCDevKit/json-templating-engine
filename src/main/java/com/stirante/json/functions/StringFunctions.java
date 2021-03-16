package com.stirante.json.functions;

import com.stirante.json.JsonProcessor;
import org.json.JSONArray;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class StringFunctions {

    public static void register() {
        JsonProcessor.defineFunction(
                new JsonProcessor.FunctionDefinition("replace")
                        .implementation(StringFunctions::replace, String.class, String.class, String.class)
        );
        JsonProcessor.defineFunction(
                new JsonProcessor.FunctionDefinition("join")
                        .implementation(StringFunctions::join, JSONArray.class, String.class)
        );
        JsonProcessor.defineFunction(
                new JsonProcessor.FunctionDefinition("contains")
                        .implementation(StringFunctions::contains, String.class, String.class)
        );
        JsonProcessor.defineFunction(
                new JsonProcessor.FunctionDefinition("split")
                        .implementation(StringFunctions::split, String.class, String.class)
        );
        JsonProcessor.defineFunction(
                new JsonProcessor.FunctionDefinition("indexOf")
                        .implementation(StringFunctions::indexOf, String.class, String.class)
        );
    }

    private static Object replace(Object[] params) {
        return ((String) params[0]).replace((String) params[1], ((String) params[2]));
    }

    private static Object join(Object[] params) {
        return StreamSupport.stream(((JSONArray) params[0]).spliterator(), false)
                .map(Object::toString)
                .collect(Collectors.joining((String)params[1]));
    }

    private static Object contains(Object[] params) {
        return ((String) params[0]).contains((String) params[1]);
    }

    private static Object split(Object[] params) {
        return new JSONArray(Arrays.asList(((String) params[0]).split((String) params[1])));
    }

    private static Object indexOf(Object[] params) {
        return ((String) params[0]).indexOf((String) params[1]);
    }
}
