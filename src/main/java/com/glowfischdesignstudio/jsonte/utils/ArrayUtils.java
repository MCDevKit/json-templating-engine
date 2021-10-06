package com.glowfischdesignstudio.jsonte.utils;

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
}
