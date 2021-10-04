package com.glowfischdesignstudio.jsonte.utils;

import com.stirante.justpipe.function.IOFunction;

import java.nio.charset.StandardCharsets;

public class StringUtils {

    public static String def(String original, String def) {
        return original == null || original.isEmpty() ? def : original;
    }

    public static String unescape(String str) {
        if (str.length() < 3) {
            return "";
        }
        return str
                // Trim quotes
                .substring(1, str.length() - 1)
                // Escape quotes
                .replaceAll("\\\\\"", "\"")
                // Escape apostrophes
                .replaceAll("\\\\'", "'")
                // Escape newlines
                .replaceAll("\\\\n", "\n")
                // Escape backslash
                .replaceAll("\\\\\\\\", "\\\\");
    }

    public static IOFunction<byte[], byte[]> replace(String... params) {
        if (params.length % 2 != 0) {
            throw new IllegalArgumentException("Number of arguments is not even!");
        }
        return bytes -> {
            String s = new String(bytes);
            for (int i = 0; i < params.length; i += 2) {
                s = s.replaceAll("%" + params[i] + "%", params[i + 1]);
            }
            return s.getBytes(StandardCharsets.UTF_8);
        };
    }

    public static String getFormatterNumber(int i) {
        if (i == 1 || (i > 20 && (i % 10) == 1)) {
            return i + "st";
        }
        if (i == 2 || (i > 20 && (i % 10) == 2)) {
            return i + "nd";
        }
        if (i == 3 || (i > 20 && (i % 10) == 3)) {
            return i + "rd";
        }
        return i + "th";
    }
}
