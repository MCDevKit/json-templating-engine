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

/**
 * Functions related to files and file paths.
 */
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

    /**
     * Returns an array of file names.
     * @param path path: A path to the folder
     * @param filter filter: (optional) A wildcard filter for filtering the file list
     * @example
     * <code>
     * {
     *   "$template": {
     *     "{{#fileList('resources/textures/particle', '*.png')}}": {
     *       "{{index}}": "{{value}}"
     *     }
     *   }
     * }
     * </code>
     */
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

    /**
     * Returns an array of file paths recursively, which means, that if the folder contains another folder, its contents will also be added to the array.
     * @param path path: A path to the folder
     * @param filter filter: (optional) A wildcard filter for filtering the file list
     * @example
     * <code>
     * {
     *   "$template": {
     *     "{{#fileListRecurse('resources/textures', '*.png')}}": {
     *       "{{index}}": "{{value}}"
     *     }
     *   }
     * }
     * </code>
     */
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

    /**
     * Returns an extension from file path in first argument.
     * @param path path: A path to the file
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be 'png'",
     *     "test": "{{fileExtension('resources/textures/particle/particles.png')}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    private static String fileExtension(String path) {
        return FilenameUtils.getExtension(path);
    }

    /**
     * Returns a name and extension from file path in first argument.
     * @param path path: A path to the file
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be 'particles.png'",
     *     "test": "{{fileName('resources/textures/particle/particles.png')}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    private static String fileName(String path) {
        return FilenameUtils.getName(path);
    }

    /**
     * Returns a name from file path in first argument.
     * @param path path: A path to the file
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be 'particles'",
     *     "test": "{{fileBaseName('resources/textures/particle/particles.png')}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    private static String fileBaseName(String path) {
        return FilenameUtils.getBaseName(path);
    }

    /**
     * Returns a directory path from file path in first argument.
     * @param path path: A path to the file
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be 'resources/textures/particle/'",
     *     "test": "{{filePath('resources/textures/particle/particles.png')}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    private static String filePath(String path) {
        return FilenameUtils.getPath(path);
    }
}
