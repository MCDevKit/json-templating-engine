package com.stirante.json.functions.impl;

import com.stirante.json.functions.JSONFunction;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Utility functions are related to manipulating scope data.
 */
public class UtilityFunctions {

    /**
     * Returns an array, where each element is replaced with the value from the specified map.
     * @param arr array: Array to be mapped
     * @param map map: Map object
     * @deprecated
     * @example
     * Given scope
     * <code>
     * {
     *   "map": {
     *     "1": "someVal",
     *     "2": "anotherVal"
     *   },
     *   "testArray": ["1", "2"]
     * }
     * </code>
     * for query
     * <code>
     * {
     *   "$template": {
     *     "test": "{{mapValues(testArray, map)}}"
     *   }
     * }
     * </code>
     *
     * the result will be
     * <code>
     * [
     *   "someVal",
     *   "anotherVal"
     * ]
     * </code>
     */
    @JSONFunction
    private static JSONArray mapValues(JSONArray arr, JSONObject map) {
        return new JSONArray(StreamSupport.stream(arr.spliterator(), false)
                .map(o -> map.get(o.toString()))
                .collect(Collectors.toList()));
    }

    /**
     * Returns whether the object has specified field.
     * @param obj object: Object to check
     * @param key field name: Field name
     * @example
     * Scope
     * <code>
     * {
     *   "map": {
     *     "1": "someVal",
     *     "2": "anotherVal"
     *   }
     * }
     * </code>
     *
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be false",
     *     "test": "{{exists(map, '3')}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    private static Boolean exists(JSONObject obj, String key) {
        return obj.has(key);
    }

}
