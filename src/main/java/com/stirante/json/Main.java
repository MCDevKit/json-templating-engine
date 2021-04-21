package com.stirante.json;

import com.stirante.json.exception.JsonTemplatingException;
import com.stirante.json.utils.FileUtils;
import com.stirante.json.utils.PipeExtensions;
import com.stirante.justpipe.Pipe;
import com.stirante.justpipe.exception.RuntimeIOException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
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
            int i = 1;
            for (; i < args.length; i++) {
                if (!args[i].startsWith("--")) {
                    break;
                }
                if (args.length > i + 1) {
                    if (args[i].equalsIgnoreCase("--scope")) {
                        i++;
                        try {
                            scope = Pipe.from(new File(args[i])).to(PipeExtensions.JSON_OBJECT);
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
                }
            }
            if (action.equalsIgnoreCase("eval")) {
                if (args.length > i) {
                    Object value = JsonProcessor.resolve(args[i], scope, "#/").getValue();
                    System.out.println(value);
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
                if (args.length > i) {
                    List<File> files = new ArrayList<>();
                    for (; i < args.length; i++) {
                        files.add(new File(args[i]));
                    }
                    files = files.stream().filter(File::exists).flatMap(FileUtils::expand).collect(Collectors.toList());
                    JSONObject finalScope = scope;
                    File finalOut = out;
                    files.stream().filter(file -> file.getName().endsWith(".modl")).forEach(file -> {
                        try {
                            JsonProcessor.processModule(Pipe.from(file).toString(), finalScope, 0);
                        } catch (IOException e) {
                            throw new JsonTemplatingException("Failed to process file " + file.getName(), e);
                        }
                    });
                    files.stream().filter(file -> file.getName().endsWith(".templ")).forEach(file -> {
                        try {
                            String name = file.getName().substring(0, file.getName().lastIndexOf('.'));
                            Pipe.from(file)
                                    .split(pipe -> JsonProcessor.processJson(name, pipe.toString(), finalScope, 0)
                                            .entrySet()
                                            .stream()
                                            .map(e -> Pipe.from(e.getValue()).with("name", e.getKey()))
                                            .collect(Collectors.toList()))
                                    .forEach(pipe -> {
                                        if (finalOut != null && finalOut.exists()) {
                                            RuntimeIOException.wrap(() -> pipe.to(new File(finalOut,
                                                    pipe.get("name") + ".json")));
                                        }
                                        else {
                                            System.out.println(pipe.get("name") + ".json" + ":");
                                            System.out.println(pipe.toString());
                                        }
                                    });
                        } catch (IOException e) {
                            throw new JsonTemplatingException("Failed to process file " + file.getName(), e);
                        }
                    });
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
        System.out.flush();
    }

}
