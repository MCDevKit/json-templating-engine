package com.glowfischdesignstudio.jsonte.functions.impl;

import com.glowfischdesignstudio.jsonte.functions.JSONFunction;
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

    /**
     * Returns the specified field value or default if doesn't exist or null.
     * @param obj object: Object to check
     * @param key field name: Field name
     * @param def default value: Value returned if the field doesn't exist or null
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
     *     "$comment": "The field below will be 'defaultVal'",
     *     "test": "{{def(map, '3', 'defaultVal')}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    private static Object def(JSONObject obj, String key, Object def) {
        return obj.has(key) && obj.get(key) != null ? obj.get(key) : def;
    }

}
