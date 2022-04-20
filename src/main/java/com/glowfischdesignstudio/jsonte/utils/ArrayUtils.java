package com.glowfischdesignstudio.jsonte.utils;

import org.json.JSONArray;

import java.util.Arrays;

public class ArrayUtils {

    public static <T> T[] concat(T[] array1, T[] array2) {
        T[] result = Arrays.copyOf(array1, array1.length + array2.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    public static <T> T[] prepend(T element, T[] array2) {
        T[] result = Arrays.copyOf(array2, 1 + array2.length);
        System.arraycopy(array2, 0, result, 1, array2.length);
        result[0] = element;
        return result;
    }

    /**
     * Returns a JSON array that contains all numbers between start and end, inclusive.
     * @param start The first number in the array.
     * @param end The last number in the array.
     * @return A JSON array that contains all numbers between start and end, inclusive.
     */
    public static JSONArray range(int start, int end) {
        JSONArray arr = new JSONArray();
        if (start > end) {
            return arr;
        }
        for (int i = start; i <= end; i++) {
            arr.put(i);
        }
        return arr;
    }

}
