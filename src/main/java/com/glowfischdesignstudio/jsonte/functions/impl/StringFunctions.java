package com.glowfischdesignstudio.jsonte.functions.impl;

import com.glowfischdesignstudio.jsonte.functions.JSONFunction;
import com.glowfischdesignstudio.jsonte.functions.JSONInstanceFunction;
import org.json.JSONArray;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * String functions are related to manipulating string values.
 */
public class StringFunctions {

    /**
     * Returns string from the first argument with occurrences of the second argument replaced by third argument.
     * @param str original string: String to be modified
     * @param toReplace target: String to be replaced
     * @param replacement replacement: Replacement string
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be 'this_is_a_test'",
     *     "test": "{{replace('this is a test', ' ', '_')}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONInstanceFunction
    private static String replace(String str, String toReplace, String replacement) {
        return str.replace(toReplace, replacement);
    }

    /**
     * Joins all values in a given array and returns as a string.
     * @param arr values to join: Array of values to join
     * @param delimiter delimiter: String inserted in between the values
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "For an array ['this', 'is', 'a', 'test'] the field below will be 'this_is_a_test'",
     *     "test": "{{join(arr, '_')}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONInstanceFunction
    private static String join(JSONArray arr, String delimiter) {
        return StreamSupport.stream(arr.spliterator(), false)
                .map(Object::toString)
                .collect(Collectors.joining(delimiter));
    }

    /**
     * Returns whether first string argument contains second string argument.
     * @param str string: Text, on which the search is executed
     * @param toFind string to search: Text to search
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be true",
     *     "test": "{{contains('this_is_a_test', '_')}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONInstanceFunction
    private static Boolean contains(String str, String toFind) {
        return str.contains(toFind);
    }

    /**
     * Returns an array from string.
     * @param str string: Text to split
     * @param delimiter delimiter: Text to split by
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be ['this', 'is', 'a', 'test']",
     *     "test": "{{split('this_is_a_test', '_')}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONInstanceFunction
    private static JSONArray split(String str, String delimiter) {
        return new JSONArray(Arrays.asList(str.split(delimiter)));
    }

    /**
     * Returns an index of second string inside the firs string or -1 if not found.
     * @param str string: Text to search inside
     * @param toFind string to search: Text to search
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be 4",
     *     "test": "{{indexOf('this_is_a_test', '_is_')}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONInstanceFunction
    private static Integer indexOf(String str, String toFind) {
        return str.indexOf(toFind);
    }

    /**
     * Returns an integer hash of the string. Note that 32 bit size does not guarantee, the generated hash will always be unique.
     * @param str string: Text to hash
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be -1238115836",
     *     "test": "{{hash('this_is_a_test'))}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONInstanceFunction
    private static Integer hash(String str) {
        return str.hashCode() % Integer.MAX_VALUE;
    }

    /**
     * Returns a string, where all letters are uppercase.
     * @param str string: Text to transform
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be TEST",
     *     "test": "{{toUpperCase('Test'))}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONInstanceFunction
    private static String toUpperCase(String str) {
        return str.toUpperCase(Locale.ROOT);
    }

    /**
     * Returns a string, where all letters are lowercase.
     * @param str string: Text to transform
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be test",
     *     "test": "{{toUpperCase('Test'))}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONInstanceFunction
    private static String toLowerCase(String str) {
        return str.toLowerCase(Locale.ROOT);
    }

    /**
     * Returns a string that is a substring of this string. The substring begins at the specified beginIndex and extends to the character at index endIndex - 1 or to the end of the string, if none specified.
     * @param str string: Text to transform
     * @param start beginIndex: the beginning index, inclusive
     * @param end endIndex: (optional) the ending index, exclusive
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be 45",
     *     "test": "{{substring('123456789', 3, 5))}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONInstanceFunction
    private static String substring(String str, Number start, Number end) {
        return str.substring(start.intValue(), end.intValue());
    }

    @JSONFunction
    @JSONInstanceFunction
    private static String substring(String str, Number start) {
        return str.substring(start.intValue());
    }

    /**
     * Returns a string, where all letters are lowercase, and the first letter is uppercase.
     * @param str string: Text to transform
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be Test",
     *     "test": "{{capitalize('test'))}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONInstanceFunction
    private static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1).toLowerCase(Locale.ROOT);
    }

    /**
     * Tests if this string starts with the specified prefix.
     * @param str string: Text to check
     * @param prefix string: Prefix
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be true",
     *     "test": "{{startsWith('testing', 'test'))}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONInstanceFunction
    private static Boolean startsWith(String str, String prefix) {
        return str.startsWith(prefix);
    }

    /**
     * Tests if this string ends with the specified suffix.
     * @param str string: Text to check
     * @param suffix string: Suffix
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be true",
     *     "test": "{{endsWith('testing', 'ing'))}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONInstanceFunction
    private static Boolean endsWith(String str, String suffix) {
        return str.startsWith(suffix);
    }

    /**
     * Replaces each substring of this string that matches the given regular expression with the given replacement.
     * @param str string: Text to transform
     * @param regex string: Regular expression
     * @param replacement string: Replacement
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be player.first_person",
     *     "test": "{{regexReplace('controller.animation.player.first_person', '(?:controller\.)?(?:animation\.)(.+)', '$1'))}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONInstanceFunction
    private static String regexReplace(String str, String regex, String replacement) {
        return str.replaceAll(regex, replacement);
    }

    /**
     * Returns an array of strings, where each string is another character.
     * @param str string: Text convert
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be ['h', 'e', 'l', 'l', 'o']",
     *     "test": "{{chars('hello')}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONInstanceFunction
    private static JSONArray chars(String str) {
        return new JSONArray(str.chars().mapToObj(c -> "" + (char) c).collect(Collectors.toList()));
    }
}
