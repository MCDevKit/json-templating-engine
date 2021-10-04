package com.glowfischdesignstudio.jsonte.functions.impl;

import com.glowfischdesignstudio.jsonte.functions.JSONFunction;
import com.glowfischdesignstudio.jsonte.utils.Pair;
import com.glowfischdesignstudio.jsonte.exception.JsonTemplatingException;
import com.glowfischdesignstudio.jsonte.utils.ImageUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Image functions are related to reading various information about images.
 */
public class ImageFunctions {

    /**
     * Returns an image width from file path in first argument.
     * @param path path: A path to the image
     * @example
     * <code>
     * {
     *   "$template": {
     *     "test": "{{imageWidth('resources/textures/particle/particles.png')}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    private static Integer imageWidth(String path) {
        File f = new File(path);
        if (!f.exists()) {
            throw new JsonTemplatingException(String.format("File '%s' not found!", path));
        }
        try {
            Pair<Integer, Integer> bounds = ImageUtils.getBounds(f);
            return bounds.getKey();
        } catch (IllegalArgumentException | IOException e) {
            try {
                BufferedImage read = ImageIO.read(f);
                return read.getWidth();
            } catch (IOException ioException) {
                throw new JsonTemplatingException("Failed to read the image!", ioException);
            }
        }
    }

    /**
     * Returns an image height from file path in first argument.
     * @param path path: A path to the image
     * @example
     * <code>
     * {
     *   "$template": {
     *     "test": "{{imageWidth('resources/textures/particle/particles.png')}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    private static Integer imageHeight(String path) {
        File f = new File(path);
        if (!f.exists()) {
            throw new JsonTemplatingException(String.format("File '%s' not found!", path));
        }
        try {
            Pair<Integer, Integer> bounds = ImageUtils.getBounds(f);
            return bounds.getValue();
        } catch (IllegalArgumentException | IOException e) {
            try {
                BufferedImage read = ImageIO.read(f);
                return read.getHeight();
            } catch (IOException ioException) {
                throw new JsonTemplatingException("Failed to read the image!", ioException);
            }
        }
    }

}
