package com.glowfischdesignstudio.jsonte.functions.impl;

import com.glowfischdesignstudio.jsonte.exception.JsonTemplatingException;
import com.glowfischdesignstudio.jsonte.functions.JSONFunction;
import com.glowfischdesignstudio.jsonte.functions.JSONUnsafe;
import com.glowfischdesignstudio.jsonte.utils.PipeExtensions;
import com.glowfischdesignstudio.jsonte.utils.Semver;
import com.stirante.justpipe.Pipe;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Functions specific to Minecraft.
 */
public class MinecraftFunctions {

    private static final String VANILLA_RP_UUID = "0575c61f-a5da-4b7f-9961-ffda2908861e";
    private static final String VANILLA_BP_UUID = "fe9f8597-5454-481a-8730-8d070a8e2e58";

    private static String installDir = null;
    private static NavigableMap<Semver, File> rpVersions = null;
    private static NavigableMap<Semver, File> bpVersions = null;

    /**
     * Returns a path to the folder with Minecraft app. The value is cached after the first usage.
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will most likely be 'C:\Program Files\WindowsApps\Microsoft.MinecraftUWP_<Minecraft version>__8wekyb3d8bbwe'",
     *     "test": "{{getMinecraftInstallDir()}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONUnsafe
    private static String getMinecraftInstallDir() {
        if (installDir == null) {
            try {
                String command = "powershell.exe (Get-AppxPackage -Name Microsoft.MinecraftUWP).InstallLocation";
                Process powerShellProcess = Runtime.getRuntime().exec(command);
                powerShellProcess.getOutputStream().close();
                installDir = Pipe.from(powerShellProcess.getInputStream()).toString().trim().replaceAll("\n", "");
            } catch (Throwable e) {
                throw new JsonTemplatingException("Failed to get Minecraft install directory!", e);
            }
        }
        return installDir;
    }

    /**
     * Returns a path to the latest vanilla behavior pack file.
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will most likely be 'C:\Program Files\WindowsApps\Microsoft.MinecraftUWP_<Minecraft version>__8wekyb3d8bbwe\data\behavior_packs\vanilla_1.18.10\entities\axolotl.json'",
     *     "test": "{{getLatestBPFile('entities/axolotl.json')}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONUnsafe
    private static String getLatestBPFile(String path) {
        if (bpVersions == null) {
            bpVersions = findPackVersions(true, VANILLA_BP_UUID);
        }
        return getLatestFile(path, bpVersions);
    }

    /**
     * Returns a path to the latest vanilla resource pack file.
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will most likely be 'C:\Program Files\WindowsApps\Microsoft.MinecraftUWP_<Minecraft version>__8wekyb3d8bbwe\data\resource_packs\vanilla_1.17.0\entity\axolotl.entity.json'",
     *     "test": "{{getLatestRPFile('entity/axolotl.entity.json')}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONUnsafe
    private static String getLatestRPFile(String path) {
        if (rpVersions == null) {
            rpVersions = findPackVersions(false, VANILLA_RP_UUID);
        }
        return getLatestFile(path, rpVersions);
    }

    /**
     * Returns an array of paths to the latest vanilla resource pack files in a given directory.
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be an array of absolute paths to latest camera files across all vanilla packs",
     *     "test": "{{listLatestRPFiles('cameras')}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONUnsafe
    private static JSONArray listLatestRPFiles(String path) {
        if (rpVersions == null) {
            rpVersions = findPackVersions(false, VANILLA_RP_UUID);
        }
        return new JSONArray(listLatestFile(path, rpVersions));
    }

    /**
     * Returns an array of paths to the latest vanilla behavior pack files in a given directory.
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be an array of absolute paths to latest spawn rules files across all vanilla packs",
     *     "test": "{{listLatestBPFiles('spawn_rules')}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    @JSONUnsafe
    private static JSONArray listLatestBPFiles(String path) {
        if (bpVersions == null) {
            bpVersions = findPackVersions(true, VANILLA_BP_UUID);
        }
        return new JSONArray(listLatestFile(path, bpVersions));
    }

    private static List<String> listLatestFile(String path, NavigableMap<Semver, File> versions) {
        Map.Entry<Semver, File> entry = versions.firstEntry();
        if (entry == null) {
            throw new JsonTemplatingException("Failed to find the packs!");
        }
        File base = entry.getValue().getParentFile();
        List<String> result = new ArrayList<>();
        do {
            File f = new File(entry.getValue(), path);
            if (!f.exists()) {
                continue;
            }
            try {
                List<File> files = Files.walk(f.toPath())
                        .map(Path::toFile)
                        .filter(File::isFile)
                        .collect(Collectors.toList());
                for (File file : files) {
                    String relativePath = entry.getValue().toPath().relativize(file.toPath()).toString();
                    result.removeIf(s -> s.endsWith(relativePath));
                    result.add(base.toPath().relativize(file.toPath()).toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        while ((entry = versions.higherEntry(entry.getKey())) != null);
        return result.stream().map(s -> new File(base, s).getAbsolutePath()).collect(Collectors.toList());
    }

    private static String getLatestFile(String path, NavigableMap<Semver, File> versions) {
        Map.Entry<Semver, File> entry = versions.lastEntry();
        if (entry == null) {
            throw new JsonTemplatingException("Failed to find the the packs!");
        }
        File result = new File(entry.getValue(), path);
        while (!result.exists()) {
            entry = versions.lowerEntry(entry.getKey());
            if (entry == null) {
                throw new JsonTemplatingException("Failed to find the file '" + path + "'");
            }
            result = new File(entry.getValue(), path);
        }
        return result.getAbsolutePath();
    }

    private static NavigableMap<Semver, File> findPackVersions(boolean isBehaviorPack, String uuid) {
        File packs = new File(getMinecraftInstallDir() + "\\data\\" + (isBehaviorPack ? "behavior_packs" : "resource_packs") + "\\");
        File[] files = packs.listFiles();
        if (files == null) {
            throw new JsonTemplatingException("Failed to get behavior packs folder!");
        }
        NavigableMap<Semver, File> versions = new TreeMap<>(Semver::compareTo);
        for (File file : files) {
            if (file.isDirectory()) {
                try {
                    JSONObject json = Pipe.from(new File(file, "manifest.json")).to(PipeExtensions.JSON_OBJECT);
                    if (json.has("header")) {
                        JSONObject header = json.getJSONObject("header");
                        if (header.has("uuid") &&
                                header.getString("uuid").equals(uuid) && header.has("version")) {
                            JSONArray version = header.getJSONArray("version");
                            versions.put(new Semver(version.getInt(0), version.getInt(1), version.getInt(2)), file);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return versions;
    }

}
