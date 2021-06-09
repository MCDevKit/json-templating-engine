package com.stirante.json.functions.impl;

import com.stirante.json.functions.JSONFunction;
import org.json.JSONArray;

import java.awt.Color;
import java.util.Arrays;

/**
 * Color functions are related to converting and manipulating colors.
 */
public class ColorFunctions {

    /**
     * Returns a color array from hex color string in first argument.
     * @param hex hex color: A color in hex format
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be [0.2, 0.4, 0.6, 1]",
     *     "test": "{{hexToArray('#336699')}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    private static JSONArray hexToArray(String hex) {
        Color color = Color.decode(hex);
        return new JSONArray(
                Arrays.asList(
                        color.getRed() / 255d,
                        color.getGreen() / 255d,
                        color.getBlue() / 255d,
                        color.getAlpha() / 255d)
        );
    }

}
