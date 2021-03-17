package com.stirante.json.functions;

import org.json.JSONArray;

import java.awt.Color;
import java.util.Arrays;

public class ColorFunctions {

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
