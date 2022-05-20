package com.glowfischdesignstudio.jsonte;

import com.glowfischdesignstudio.jsonte.utils.StringUtils;
import com.stirante.justpipe.Pipe;
import org.json.JSONObject;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JsonProcessorTest {

    @TestFactory
    public Collection<DynamicTest> executeAllTests() throws Exception {
        Collection<DynamicTest> dynamicTests = new ArrayList<>();
        File f = new File("src/test/resources/dynamic");
        File[] files = f.listFiles(pathname -> pathname.getName().endsWith(".templ"));
        if (files != null) {
            for (File file : files) {
                File expected = new File(file.getParentFile(), file.getName() + "expected");
                if (!expected.exists()) {
                    System.out.println("Could not find expected file for " + file.getName() + "!");
                    System.out.println("Creating one from current result, please check if it's valid!");

                    StringWriter stringWriter = new StringWriter();
                    Pipe.from(file).to(stringWriter);
                    Map<String, Object> s = JsonProcessor.processJson(file.getName()
                            .substring(0, file.getName().lastIndexOf('.')), stringWriter.toString(), new JSONObject(), 0, new HashMap<>());
                    StringBuilder result = new StringBuilder();
                    for (String s1 : s.keySet()) {
                        result.append("File ").append(s1).append("\n");
                        result.append(StringUtils.toString(s.get(s1), 2)).append("\n");
                    }
                    Pipe.from(result.toString()).to(expected);
                }
                Executable exec = () -> {
                    StringWriter stringWriter = new StringWriter();
                    Pipe.from(file).to(stringWriter);
                    Map<String, Object> s = JsonProcessor.processJson(file.getName()
                            .substring(0, file.getName().lastIndexOf('.')), stringWriter.toString(), new JSONObject(), 0, new HashMap<>());
                    StringBuilder result = new StringBuilder();
                    for (String s1 : s.keySet()) {
                        System.out.println("File " + s1);
                        System.out.println(StringUtils.toString(s.get(s1), 2));
                        result.append("File ").append(s1).append("\n");
                        result.append(StringUtils.toString(s.get(s1), 2)).append("\n");
                    }
                    assertNotNull(s);
                    StringWriter out = new StringWriter();
                    Pipe.from(expected).to(out);
                    String loaded = out.toString();
                    assertEquals(loaded, result.toString());
                };
                String testName = file.getName().substring(0, file.getName().lastIndexOf('.'));
                DynamicTest dTest = DynamicTest.dynamicTest(testName, exec);
                dynamicTests.add(dTest);
            }
        }
        return dynamicTests;
    }

}