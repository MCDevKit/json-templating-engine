package com.stirante.json.functions;

import com.stirante.json.exception.JsonTemplatingException;
import com.stirante.json.JsonProcessor;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

public class FileFunctions {

    public static void register() {
        JsonProcessor.defineFunction(
                new JsonProcessor.FunctionDefinition("fileList")
                        .implementation(FileFunctions::fileList, String.class)
                        .implementation(FileFunctions::fileListFilter, String.class, String.class)
        );
        JsonProcessor.defineFunction(
                new JsonProcessor.FunctionDefinition("fileListRecurse")
                        .implementation(FileFunctions::fileListRecurse, String.class)
                        .implementation(FileFunctions::fileListRecurseFilter, String.class, String.class)
        );
        JsonProcessor.defineFunction(
                new JsonProcessor.FunctionDefinition("fileExtension")
                        .implementation(FileFunctions::fileExtension, String.class)
        );
        JsonProcessor.defineFunction(
                new JsonProcessor.FunctionDefinition("fileName")
                        .implementation(FileFunctions::fileName, String.class)
        );
        JsonProcessor.defineFunction(
                new JsonProcessor.FunctionDefinition("fileBaseName")
                        .implementation(FileFunctions::fileBaseName, String.class)
        );
        JsonProcessor.defineFunction(
                new JsonProcessor.FunctionDefinition("filePath")
                        .implementation(FileFunctions::filePath, String.class)
        );
    }

    private static Object fileList(Object[] params) {
        File f = new File(params[0].toString());
        String[] list = f.list();
        if (list == null) {
            return new JSONArray();
        }
        return new JSONArray(list);
    }

    private static Object fileListFilter(Object[] params) {
        File f = new File(params[0].toString());
        String[] list = f.list();
        if (list == null) {
            return new JSONArray();
        }
        return new JSONArray(Arrays.stream(list)
                .filter(s -> FilenameUtils.wildcardMatch(s, String.valueOf(params[1])))
                .collect(Collectors.toList()));
    }

    private static Object fileListRecurse(Object[] params) {
        File f = new File(params[0].toString());
        try {
            return new JSONArray(Files.walk(f.toPath())
                    .filter(path -> !path.toFile().isDirectory())
                    .map(path -> f.toPath().relativize(path))
                    .map(Path::toString)
                    .map(s -> s.replaceAll("\\\\", "/"))
                    .collect(Collectors.toList()));
        } catch (IOException e) {
            throw new JsonTemplatingException("An exception occurred while executing function 'fileListRecurse'", e);
        }
    }

    private static Object fileListRecurseFilter(Object[] params) {
        File f = new File(params[0].toString());
        try {

            return new JSONArray(Files.walk(f.toPath())
                    .filter(path -> !path.toFile().isDirectory())
                    .map(path -> f.toPath().relativize(path))
                    .map(Path::toString)
                    .map(s -> s.replaceAll("\\\\", "/"))
                    .filter(s -> FilenameUtils.wildcardMatch(s, String.valueOf(params[1])))
                    .collect(Collectors.toList()));
        } catch (IOException e) {
            throw new JsonTemplatingException("An exception occurred while executing function 'fileListRecurse'", e);
        }
    }

    private static Object fileExtension(Object[] params) {
        return FilenameUtils.getExtension((String) params[0]);
    }

    private static Object fileName(Object[] params) {
        return FilenameUtils.getName((String) params[0]);
    }

    private static Object fileBaseName(Object[] params) {
        return FilenameUtils.getBaseName((String) params[0]);
    }

    private static Object filePath(Object[] params) {
        return FilenameUtils.getPath((String) params[0]);
    }
}
