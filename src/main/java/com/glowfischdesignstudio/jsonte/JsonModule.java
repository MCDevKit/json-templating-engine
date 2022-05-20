package com.glowfischdesignstudio.jsonte;

import org.json.JSONObject;

public class JsonModule {

    private final JSONObject template;
    private final JSONObject scope;
    private final String name;

    public JsonModule(String name, JSONObject template, JSONObject scope) {
        this.name = name;
        this.template = template;
        this.scope = scope;
    }

    public JSONObject getTemplate() {
        return template;
    }

    public JSONObject getScope() {
        return scope;
    }

    public String getName() {
        return name;
    }
}
