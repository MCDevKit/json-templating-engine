package com.stirante.json.utils;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Stream;

public class FileUtils {

    public static Stream<File> expand(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                return Arrays.stream(files).flatMap(FileUtils::expand);
            }
            else {
                return Stream.empty();
            }
        }
        else {
            return Stream.of(file);
        }
    }

}
