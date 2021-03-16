package com.stirante.json;

public class ReferenceResult {
    private final Object value;
    private final JsonProcessor.Action action;
    private final String name;

    ReferenceResult(Object value, JsonProcessor.Action action, String name) {
        this.value = value;
        this.action = action;
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public JsonProcessor.Action getAction() {
        return action;
    }

    public String getName() {
        return name;
    }
}
