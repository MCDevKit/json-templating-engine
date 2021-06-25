package com.stirante.json.functions.impl;

import com.stirante.json.exception.JsonTemplatingException;
import com.stirante.json.functions.JSONFunction;
import com.stirante.justpipe.Pipe;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Functions specific to Minecraft.
 */
public class MinecraftFunctions {

    private static String installDir = null;

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

}
