package com.glowfischdesignstudio.jsonte;

import com.glowfischdesignstudio.jsonte.exception.JsonTemplatingException;
import com.glowfischdesignstudio.jsonte.utils.FileUtils;
import com.glowfischdesignstudio.jsonte.utils.JsonUtils;
import com.glowfischdesignstudio.jsonte.utils.PipeExtensions;
import com.glowfischdesignstudio.jsonte.utils.StringUtils;
import com.stirante.justpipe.Pipe;
import com.stirante.justpipe.exception.RuntimeIOException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        if (args.length > 0) {
            String action = args[0];
            JSONObject scope = new JSONObject();
            File out = null;
            List<String> input = new ArrayList<>();
            List<PathMatcher> include = new ArrayList<>();
            List<PathMatcher> exclude = new ArrayList<>();
            boolean removeSource = false;
            int indent = 2;
            int i = 1;
            for (; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("--remove-src")) {
                    removeSource = true;
                }
                else if (args[i].equalsIgnoreCase("--minify")) {
                    indent = 0;
                }
                else if (args[i].startsWith("--") && args.length > i + 1) {
                    if (args[i].equalsIgnoreCase("--scope")) {
                        i++;
                        try {
                            File in = new File(args[i]);
                            if (in.exists()) {
                                BasicFileAttributes attributes =
                                        Files.readAttributes(in.toPath(), BasicFileAttributes.class);
                                if (attributes.isRegularFile()) {
                                    try {
                                        JsonUtils.merge(scope, Pipe.from(in).to(PipeExtensions.JSON_OBJECT));
                                    } catch (RuntimeIOException e) {
                                        throw new RuntimeException("Failed to read file: " + in.getAbsolutePath(), e);
                                    }
                                }
                                else if (attributes.isDirectory()) {
                                    File[] files = in.listFiles(pathname -> pathname.getName().endsWith(".json"));
                                    if (files != null) {
                                        for (File file : files) {
                                            try {
                                                JsonUtils.merge(scope, Pipe.from(file).to(PipeExtensions.JSON_OBJECT));
                                            } catch (RuntimeIOException e) {
                                                throw new RuntimeException("Failed to read file: " + file.getAbsolutePath(), e);
                                            }
                                        }
                                    }
                                }
                            }
                            else {
                                System.out.println("Could not find scope file/directory: " + in.getAbsolutePath());
                            }
                        } catch (IOException e) {
                            throw new JsonTemplatingException("Failed to load the scope", e);
                        }
                    }
                    else if (args[i].equalsIgnoreCase("--out")) {
                        i++;
                        out = new File(args[i]);
                        if (!out.exists()) {
                            out.mkdirs();
                        }
                        if (!out.isDirectory()) {
                            throw new JsonTemplatingException("Output file is not a directory");
                        }
                    }
                    else if (args[i].equalsIgnoreCase("--include")) {
                        i++;
                        try {
                            include.add(FileSystems.getDefault().getPathMatcher("glob:" + args[i]));
                        } catch (Exception e) {
                            throw new JsonTemplatingException("Failed to parse include pattern \"" + args[i] + "\"", e);
                        }
                    }
                    else if (args[i].equalsIgnoreCase("--exclude")) {
                        i++;
                        try {
                            exclude.add(FileSystems.getDefault().getPathMatcher("glob:" + args[i]));
                        } catch (Exception e) {
                            throw new JsonTemplatingException("Failed to parse exclude pattern \"" + args[i] + "\"", e);
                        }
                    }
                }
                else {
                    input.add(args[i]);
                }
            }
            if (action.equalsIgnoreCase("eval")) {
                if (input.size() > 0) {
                    for (String s : input) {
                        Object value = JsonProcessor.resolve(s, scope, "#/").getValue();
                        System.out.println(value);
                    }
                }
                else {
                    System.out.println("Enter 'exit' to stop the REPL");
                    System.out.print("> ");
                    Scanner sc = new Scanner(System.in);
                    while (sc.hasNextLine()) {
                        String line = sc.nextLine();
                        if (line.equals("exit")) {
                            return;
                        }
                        try {
                            Object value = JsonProcessor.resolve(line, scope, "#/").getValue();
                            System.out.println(value);
                        } catch (JsonTemplatingException ex) {
                            System.err.println(ex.getMessage());
                            System.out.println();
                        }
                        System.out.print("> ");
                    }
                }
            }
            else if (action.equalsIgnoreCase("compile")) {
                if (input.size() > 0) {
                    List<File> files = input.stream()
                            .map(File::new)
                            .filter(File::exists)
                            .flatMap(FileUtils::expand)
                            .filter(f -> (include.isEmpty() && exclude.isEmpty()) ||
                                    (include.stream().anyMatch(m -> m.matches(f.toPath())) ||
                                            exclude.stream().noneMatch(m -> m.matches(f.toPath()))))
                            .collect(Collectors.toList());
                    File finalOut = out;
                    files.stream().filter(file -> file.getName().endsWith(".modl")).forEach(file -> {
                        System.out.println("Processing " + file.getName());
                        try {
                            JsonProcessor.processModule(Pipe.from(file).toString());
                        } catch (IOException e) {
                            throw new JsonTemplatingException("Failed to read file: " + file.getAbsolutePath(), e);
                        }
                    });
                    int finalIndent = indent;
                    files.stream().filter(file -> file.getName().endsWith(".templ")).forEach(file -> {
                        System.out.println("Compiling " + file.getName());
                        try {
                            String name = file.getName().substring(0, file.getName().lastIndexOf('.'));
                            Pipe.from(file)
                                    .split(pipe -> JsonProcessor.processJson(name, pipe.toString(), scope, 0)
                                            .entrySet()
                                            .stream()
                                            .map(e -> Pipe.from(StringUtils.toString(e.getValue(), finalIndent)).with("name", e.getKey()))
                                            .collect(Collectors.toList()))
                                    .forEach(pipe -> {
                                        if (finalOut != null && finalOut.exists()) {
                                            File f = new File(finalOut,
                                                    file.getParentFile().getPath() + "/" + pipe.get("name") + ".json");
                                            f.getParentFile().mkdirs();
                                            RuntimeIOException.wrap(() -> pipe.to(f));
                                        }
                                        else {
                                            System.out.println(pipe.get("name") + ".json" + ":");
                                            System.out.println(pipe);
                                        }
                                    });
                        } catch (IOException e) {
                            throw new JsonTemplatingException("Failed to read file: " + file.getAbsolutePath(), e);
                        }
                    });
                    if (removeSource) {
                        if (!files.stream()
                                .filter(file -> file.getName().endsWith(".templ") || file.getName().endsWith(".modl"))
                                .peek(file -> System.out.println("Removing " + file.getName()))
                                .allMatch(File::delete)) {
                            System.out.println("Failed to delete some files");
                        }
                    }
                }
                else {
                    throw new JsonTemplatingException("No files provided!");
                }
            }
        }
        else {
            printUsage();
        }
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("\teval <options> [expression] - evaluates expression, or if none provided, starts a simple REPL");
        System.out.println("\tcompile <options> <files>");
        System.out.println("Options:");
        System.out.println("\t--scope <file> - scope for processing");
        System.out.println("\t--out <dir> - output directory for compiled files");
        System.out.println("\t--exclude <pattern> - exclude files matching pattern");
        System.out.println("\t--include <pattern> - include files matching pattern");
        System.out.flush();
    }

}
