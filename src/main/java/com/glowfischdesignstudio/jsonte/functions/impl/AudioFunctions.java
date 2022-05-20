package com.glowfischdesignstudio.jsonte.functions.impl;

import com.glowfischdesignstudio.jsonte.JsonProcessor;
import com.glowfischdesignstudio.jsonte.exception.JsonTemplatingException;
import com.glowfischdesignstudio.jsonte.functions.JSONFunction;
import com.glowfischdesignstudio.jsonte.utils.AudioUtils;

import java.io.File;
import java.io.IOException;

/**
 * Audio functions are related to reading various information about audio files.
 */
public class AudioFunctions {

    /**
     * Returns duration of audio in seconds from file path in first argument.
     *
     * Currently, only supports PCM WAV files in RIFF format.
     *
     * @param path path: A path to the audio file
     * @example
     * <code>
     * {
     *   "$template": {
     *     "test": "{{audioDuration('resources/sounds/sound.wav')}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    private static Double audioDuration(String path) {
        try {
            return AudioUtils.getAudioInfo(JsonProcessor.FILE_LOADER.apply(path)).duration;
        } catch (IOException e) {
            throw new JsonTemplatingException("Failed to read the file", e);
        }
    }

}
