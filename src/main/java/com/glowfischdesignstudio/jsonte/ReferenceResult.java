package com.glowfischdesignstudio.jsonte;

public class ReferenceResult {
    private final Object value;
    private final JsonAction jsonAction;
    private final String name;

    ReferenceResult(Object value, JsonAction jsonAction, String name) {
        this.value = value;
        this.jsonAction = jsonAction;
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public JsonAction getAction() {
        return jsonAction;
    }

    public String getName() {
        return name;
    }
}
