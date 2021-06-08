package com.stirante.json.functions.impl;

import com.stirante.json.exception.JsonTemplatingException;
import com.stirante.json.functions.JSONFunction;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

public class FileFunctions {

    @JSONFunction
    private static JSONArray fileList(String path) {
        File f = new File(path);
        String[] list = f.list();
        if (list == null) {
            return new JSONArray();
        }
        return new JSONArray(list);
    }

    @JSONFunction
    private static JSONArray fileList(String path, String filter) {
        File f = new File(path);
        String[] list = f.list();
        if (list == null) {
            return new JSONArray();
        }
        return new JSONArray(Arrays.stream(list)
                .filter(s -> FilenameUtils.wildcardMatch(s, filter))
                .collect(Collectors.toList()));
    }

    @JSONFunction
    private static JSONArray fileListRecurse(String path) {
        File f = new File(path);
        try {
            return new JSONArray(Files.walk(f.toPath())
                    .filter(p -> !p.toFile().isDirectory())
                    .map(p -> f.toPath().relativize(p))
                    .map(Path::toString)
                    .map(s -> s.replaceAll("\\\\", "/"))
                    .collect(Collectors.toList()));
        } catch (IOException e) {
            throw new JsonTemplatingException("An exception occurred while executing function 'fileListRecurse'", e);
        }
    }

    @JSONFunction
    private static JSONArray fileListRecurse(String path, String filter) {
        File f = new File(path);
        try {

            return new JSONArray(Files.walk(f.toPath())
                    .filter(p -> !p.toFile().isDirectory())
                    .map(p -> f.toPath().relativize(p))
                    .map(Path::toString)
                    .map(s -> s.replaceAll("\\\\", "/"))
                    .filter(s -> FilenameUtils.wildcardMatch(s, filter))
                    .collect(Collectors.toList()));
        } catch (IOException e) {
            throw new JsonTemplatingException("An exception occurred while executing function 'fileListRecurse'", e);
        }
    }

    @JSONFunction
    private static String fileExtension(String path) {
        return FilenameUtils.getExtension(path);
    }

    @JSONFunction
    private static String fileName(String path) {
        return FilenameUtils.getName(path);
    }

    @JSONFunction
    private static String fileBaseName(String path) {
        return FilenameUtils.getBaseName(path);
    }

    @JSONFunction
    private static String filePath(String path) {
        return FilenameUtils.getPath(path);
    }
}
