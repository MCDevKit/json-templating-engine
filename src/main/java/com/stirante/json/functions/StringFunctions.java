package com.stirante.json.functions;

import org.json.JSONArray;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class StringFunctions {

    @JSONFunction
    private static String replace(String str, String toReplace, String replacement) {
        return str.replace(toReplace, replacement);
    }

    @JSONFunction
    private static String join(JSONArray arr, String delimiter) {
        return StreamSupport.stream(arr.spliterator(), false)
                .map(Object::toString)
                .collect(Collectors.joining(delimiter));
    }

    @JSONFunction
    private static Boolean contains(String str, String toFind) {
        return str.contains(toFind);
    }

    @JSONFunction
    private static JSONArray split(String str, String delimiter) {
        return new JSONArray(Arrays.asList(str.split(delimiter)));
    }

    @JSONFunction
    private static Integer indexOf(String str, String toFind) {
        return str.indexOf(toFind);
    }

    @JSONFunction
    private static Integer hash(String str) {
        return str.hashCode() % Integer.MAX_VALUE;
    }

    @JSONFunction
    private static String toUpperCase(String str) {
        return str.toUpperCase(Locale.ROOT);
    }

    @JSONFunction
    private static String toLowerCase(String str) {
        return str.toLowerCase(Locale.ROOT);
    }

    @JSONFunction
    private static String substring(String str, Number start, Number end) {
        return str.substring(start.intValue(), end.intValue());
    }

    @JSONFunction
    private static String substring(String str, Number start) {
        return str.substring(start.intValue());
    }

    @JSONFunction
    private static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1).toLowerCase(Locale.ROOT);
    }
}
