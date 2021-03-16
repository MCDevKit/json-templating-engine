package com.stirante.json.functions;

import com.stirante.json.exception.JsonTemplatingException;
import com.stirante.json.JsonProcessor;
import com.stirante.json.utils.ImageUtils;
import com.stirante.json.utils.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageFunctions {

    public static void register() {
        JsonProcessor.defineFunction(new JsonProcessor.FunctionDefinition("imageWidth")
                .implementation(ImageFunctions::imageWidth, String.class)
        );
        JsonProcessor.defineFunction(new JsonProcessor.FunctionDefinition("imageHeight")
                .implementation(ImageFunctions::imageHeight, String.class)
        );
    }

    private static Object imageWidth(Object[] params) {
        File f = new File((String) params[0]);
        if (!f.exists()) {
            throw new JsonTemplatingException(String.format("File '%s' not found!", params[0]));
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

    private static Object imageHeight(Object[] params) {
        File f = new File((String) params[0]);
        if (!f.exists()) {
            throw new JsonTemplatingException(String.format("File '%s' not found!", params[0]));
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
