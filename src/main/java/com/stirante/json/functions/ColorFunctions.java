package com.stirante.json.functions;

import com.stirante.json.JsonProcessor;
import org.json.JSONArray;

import java.awt.Color;
import java.util.Arrays;

public class ColorFunctions {

    public static void register() {
        JsonProcessor.defineFunction(new JsonProcessor.FunctionDefinition("hexToArray")
                .implementation(ColorFunctions::hexToArray, String.class)
        );
    }

    private static Object hexToArray(Object[] params) {
        Color color = Color.decode((String) params[0]);
        return new JSONArray(
                Arrays.asList(
                        color.getRed() / 255d,
                        color.getGreen() / 255d,
                        color.getBlue() / 255d,
                        color.getAlpha() / 255d)
        );
    }

}
