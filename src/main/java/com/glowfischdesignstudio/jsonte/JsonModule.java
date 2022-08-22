package com.glowfischdesignstudio.jsonte;

import org.json.JSONObject;

public class JsonModule {

    private final JSONObject template;
    private final JSONObject scope;
    private String copy;
    private final String name;

    public JsonModule(String name, JSONObject template, JSONObject scope, String copy) {
        this.name = name;
        this.template = template;
        this.scope = scope;
        this.copy = copy;
    }

    public JSONObject getTemplate() {
        return template;
    }

    public JSONObject getScope() {
        return scope;
    }

    public String getCopy() {
        return copy;
    }

    public String getName() {
        return name;
    }
}
