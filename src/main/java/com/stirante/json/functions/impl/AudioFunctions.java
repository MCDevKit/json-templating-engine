package com.stirante.json.functions.impl;

import com.stirante.json.exception.JsonTemplatingException;
import com.stirante.json.functions.JSONFunction;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

/**
 * Audio functions are related to reading various information about audio files.
 */
public class AudioFunctions {

    /**
     * Returns duration of audio in seconds from file path in first argument.
     *
     * @param path path: A path to the sound
     * @example <code>
     * {
     * "$template": {
     * "test": "{{audioDuration('resources/sounds/sound.wav')}}"
     * }
     * }
     * </code>
     */
    @JSONFunction
    private static Double audioDuration(String path) {
        File f = new File(path);
        if (!f.exists()) {
            throw new JsonTemplatingException(String.format("File '%s' not found!", path));
        }
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(f);
            AudioFormat format = audioInputStream.getFormat();
            long frames = audioInputStream.getFrameLength();
            return (frames + 0.0) / format.getFrameRate();
        } catch (UnsupportedAudioFileException e) {
            throw new JsonTemplatingException("Unsupported audio file", e);
        } catch (IOException e) {
            throw new JsonTemplatingException("Failed to read the file", e);
        }
    }

}
