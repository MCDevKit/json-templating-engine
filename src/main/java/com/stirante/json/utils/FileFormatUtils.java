package com.stirante.json.utils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class FileFormatUtils {
    public static boolean compare(byte[] a1, byte[] a2) {
        if (a1.length != a2.length) {
            return false;
        }
        for (int i = 0; i < a1.length; i++) {
            if (a1[i] != a2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean compare(InputStream in, byte[] a2) throws IOException {
        byte[] a1 = new byte[a2.length];
        int read = in.read(a1);
        if (read != a2.length) {
            throw new EOFException();
        }
        return compare(a1, a2);
    }

}
