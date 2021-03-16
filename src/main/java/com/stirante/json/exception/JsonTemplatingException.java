package com.stirante.json.exception;

public class JsonTemplatingException extends RuntimeException {

    public JsonTemplatingException(String message, String location, Throwable cause) {
        super(prepareMessage(message, location), cause);
    }

    public JsonTemplatingException(String message, String location) {
        this(message, location, null);
    }

    public JsonTemplatingException(String message) {
        this(message, null, null);
    }

    public JsonTemplatingException(String message, Throwable cause) {
        this(message, null, cause);
    }

    public static String prepareMessage(String message, String location) {
        return String.format("%s%s", message, location != null ? String.format(" (%s)", location) : "");
    }

    public JsonTemplatingException withPath(String path) {
        return new JsonTemplatingException(getMessage(), path, getCause());
    }
}
