package com.glowfischdesignstudio.jsonte.utils;

public class Semver implements Comparable<Semver> {

    private final int major;
    private final int minor;
    private final int patch;

    public Semver(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public static Semver fromString(String version) {
        String[] parts = version.split("\\.");
        int major = 0;
        int minor = 0;
        int patch = 0;
        try {
            major = Integer.parseInt(parts[0]);
        } catch (NumberFormatException ignored) {
        }
        if (parts.length > 1) {
            try {
                minor = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {
            }
            if (parts.length > 2) {
                try {
                    patch = Integer.parseInt(parts[2]);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return new Semver(major, minor, patch);
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }

    @Override
    public int compareTo(Semver o) {
        if (major != o.major) {
            return major - o.major;
        }
        if (minor != o.minor) {
            return minor - o.minor;
        }
        return patch - o.patch;
    }
}
