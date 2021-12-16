package com.glowfischdesignstudio.jsonte.functions.impl;

import com.glowfischdesignstudio.jsonte.functions.JSONLambda;
import com.glowfischdesignstudio.jsonte.exception.JsonTemplatingException;
import com.glowfischdesignstudio.jsonte.functions.JSONFunction;
import com.glowfischdesignstudio.jsonte.functions.JSONInstanceFunction;
import com.glowfischdesignstudio.jsonte.utils.JsonUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Array functions are for getting information on and transforming arrays.
 */
public class ArrayFunctions {

    /**
     * Returns an array of objects from an object, where each object in resulting array has fields with field name and value.
     * @param obj object: Object to be mapped
     * @param key keyName: Name of the key field
     * @param value valueName: Name of the value field
     * @example
     * Given scope
     * <code>
     * {
     *   "testObject": {
     *     "test1": "someVal",
     *     "test2": "anotherVal"
     *   }
     * }
     * </code>
     * for query
     * <code>
     * {
     *   "$template": {
     *     "test": "{{asArray(testObject, 'key', 'value')}}"
     *   }
     * }
     * </code>
     * the result will be
     * <code>
     * [
     *   {
     *     "key": "test1",
     *     "value": "someVal"
     *   },
     *   {
     *     "key": "test2",
     *     "value": "anotherVal"
     *   }
     * ]
     * </code>
     */
    @JSONFunction
    private static JSONArray asArray(JSONObject obj, String key, String value) {
        return new JSONArray(obj.keySet().stream().map(s -> {
            JSONObject r = new JSONObject();
            r.put(key, s);
            r.put(value, obj.get(s));
            return r;
        }).collect(Collectors.toList()));
    }

    /**
     * Returns an array of keys from the object.
     * @param obj object: Source object
     * @example
     * Given scope
     * <code>
     * {
     *   "testObject": {
     *     "test1": "someVal",
     *     "test2": "anotherVal"
     *   }
     * }
     * </code>
     * for query
     * <code>
     * {
     *   "$template": {
     *     "test": "{{keys(testObject)}}"
     *   }
     * }
     * </code>
     * the result will be
     * <code>
     * [
     *   "test1", "test2"
     * ]
     * </code>
     */
    @JSONFunction
    private static JSONArray keys(JSONObject obj) {
        return new JSONArray(new ArrayList<>(obj.keySet()));
    }

    /**
     * Returns an array of values from an object.
     * @param obj object: Source object
     * @example
     * Given scope
     * <code>
     * {
     *   "testObject": {
     *     "test1": "someVal",
     *     "test2": "anotherVal"
     *   }
     * }
     * </code>
     * for query
     * <code>
     * {
     *   "$template": {
     *     "test": "{{values(testObject)}}"
     *   }
     * }
     * </code>
     * the result will be
     * <code>
     * [
     *   "someVal",
     *   "anotherVal"
     * ]
     * </code>
     */
    @JSONFunction
    private static JSONArray values(JSONObject obj) {
        return new JSONArray(new ArrayList<>(obj.toMap().values()));
    }

    /**
     * Returns an array with reversed order.
     * @param arr array: Source array
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be [4, 3, 2, 1, 0]",
     *     "test": "{{reverse(0..4)}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONInstanceFunction
    private static JSONArray reverse(JSONArray arr) {
        List<?> l = new ArrayList<>(arr.toList());
        Collections.reverse(l);
        return new JSONArray(l);
    }

    /**
     * Returns whether supplied array contains provided value.
     * @param arr array: Array to check
     * @param value value: Value to search for (currently a string, or a number only)
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be true",
     *     "test": "{{contains(['asd', '123', 123, 9], '123')}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONInstanceFunction
    private static Boolean contains(JSONArray arr, String value) {
        return arr.toList().stream().anyMatch(o -> o instanceof String && o.equals(value));
    }

    @JSONFunction
    @JSONInstanceFunction
    private static Boolean contains(JSONArray arr, Number value) {
        return arr.toList()
                .stream()
                .anyMatch(o -> o instanceof Number && ((Number) o).doubleValue() == value.doubleValue());
    }

    /**
     * Returns a new array that is filtered based on a predicate.
     * @param arr array: Source array
     * @param predicate predicate(element, index): Lambda, that should return whether an element should remain
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be [0, 2, 4]",
     *     "test": "{{filter(0..4, x => mod(x, 2) == 0)}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONInstanceFunction
    private static JSONArray filter(JSONArray arr, JSONLambda predicate) {
        List<Object> objects = arr.toList();
        for (int i = objects.size() - 1; i >= 0; i--) {
            if (!JsonUtils.toBoolean(predicate.execute(objects.get(i), i))) {
                objects.remove(i);
            }
        }
        return new JSONArray(objects);
    }

    /**
     * Returns a new array, where every element is mapped to another value using provided lambda.
     * @param arr array: Source array
     * @param predicate lambda(element, index): Lambda, that should return new element value
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be [0, 2, 4, 6, 8]",
     *     "test": "{{map(0..4, x => x => x * 2)}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONInstanceFunction
    private static JSONArray map(JSONArray arr, JSONLambda predicate) {
        List<Object> objects = arr.toList();
        for (int i = 0; i < objects.size(); i++) {
            objects.set(i, predicate.execute(objects.get(i), i));
        }
        return new JSONArray(objects);
    }

    /**
     * Returns a new array, where every array from lambda is merged into source array.
     * @param arr array: Source array
     * @param predicate lambda(element, index): Lambda, that should return an array to merge. If none provided, it will use identity lambda (`x => x`)
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be [1, 2, 3, 4]",
     *     "test": "{{flatMap([[1, 2], 3, [4]])}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONInstanceFunction
    private static JSONArray flatMap(JSONArray arr, JSONLambda predicate) {
        List<Object> objects = arr.toList();
        for (int i = 0; i < objects.size(); i++) {
            objects.set(i, predicate.execute(objects.get(i), i));
        }
        return new JSONArray(objects.stream()
                .flatMap(o -> o instanceof JSONArray ?
                        ((JSONArray) o).toList().stream() : o instanceof Collection ?
                        ((Collection<?>) o).stream() : Stream.of(o))
                .collect(Collectors.toList()));
    }

    @JSONFunction
    @JSONInstanceFunction
    private static JSONArray flatMap(JSONArray arr) {
        return new JSONArray(arr.toList()
                .stream()
                .flatMap(o -> o instanceof JSONArray ? ((JSONArray) o).toList()
                        .stream() : o instanceof Collection ? ((Collection<?>) o).stream() : Stream.of(o))
                .collect(Collectors.toList()));
    }

    /**
     * Returns the number of elements in an array.
     * @param arr array: Source array
     * @param predicate predicate(element, index): (optional) Lambda, that should return whether an element should be counted
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be 3",
     *     "test": "{{count(0..4, x => mod(x, 2) == 0)}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONInstanceFunction
    private static Long count(JSONArray arr, JSONLambda predicate) {
        List<Object> objects = arr.toList();
        for (int i = objects.size() - 1; i >= 0; i--) {
            if (!JsonUtils.toBoolean(predicate.execute(objects.get(i), i))) {
                objects.remove(i);
            }
        }
        return (long) objects.size();
    }

    @JSONFunction
    @JSONInstanceFunction
    private static Integer count(JSONArray arr) {
        return arr.toList().size();
    }


    /**
     * Returns the first element from an array filtered by the predicate.
     * @param arr array: Source array
     * @param predicate predicate(element, index): Lambda, that should return whether an element should remain
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be 0",
     *     "test": "{{findFirst(0..4, x => mod(x, 2) == 0)}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONInstanceFunction
    private static Object findFirst(JSONArray arr, JSONLambda predicate) {
        List<Object> objects = arr.toList();
        for (int i = objects.size() - 1; i >= 0; i--) {
            if (JsonUtils.toBoolean(predicate.execute(objects.get(i), i))) {
                return objects.get(i);
            }
        }
        throw new JsonTemplatingException("No matching items found!");
    }


    /**
     * Returns index of given element inside the array or -1 if not found.
     * @param arr array: Source array
     * @param element element: Element to find
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be 1",
     *     "test": "{{indexOf(1..5, 2)}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONInstanceFunction
    private static Object indexOf(JSONArray arr, Object element) {
        List<Object> objects = arr.toList();
        return objects.indexOf(element);
    }

}
