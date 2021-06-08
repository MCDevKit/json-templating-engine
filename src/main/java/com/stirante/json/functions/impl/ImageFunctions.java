package com.stirante.json.functions.impl;

import com.stirante.json.exception.JsonTemplatingException;
import com.stirante.json.functions.JSONFunction;
import com.stirante.json.utils.ImageUtils;
import com.stirante.json.utils.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageFunctions {

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
