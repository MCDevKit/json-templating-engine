package com.glowfischdesignstudio.jsonte;

import org.json.JSONObject;

public class JsonModule {

    private JSONObject template;
    private JSONObject scope;

    public JsonModule(JSONObject template, JSONObject scope) {
        this.template = template;
        this.scope = scope;
    }

    public JSONObject getTemplate() {
        return template;
    }

    public JSONObject getScope() {
        return scope;
    }
}
