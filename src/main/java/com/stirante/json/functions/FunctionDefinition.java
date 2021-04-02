package com.stirante.json.functions;

import com.stirante.json.JsonProcessor;
import com.stirante.json.exception.JsonTemplatingException;
import com.stirante.json.utils.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class FunctionDefinition {
    private final List<Class<?>[]> types = new ArrayList<>();
    private final List<Function<Object[], Object>> implementations = new ArrayList<>();
    private final String name;

    public FunctionDefinition(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Object execute(Object[] params, String path) {
        for (int i = 0, typesSize = types.size(); i < typesSize; i++) {
            Class<?>[] type = types.get(i);
            if (params.length == type.length) {
                paramCheck(params, type, path);
                try {
                    return implementations.get(i).apply(params);
                } catch (JsonTemplatingException ex) {
                    throw ex.withPath(path);
                }
            }
        }
        throw new JsonTemplatingException(String.format("Incorrect number of parameters passed to function '%s'!", name), path);
    }

    private void paramCheck(Object[] params, Class<?>[] types, String path) {
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            if (!types[i].isInstance(param)) {
                if (param == null) {
                    if (types[i] == JSONArray.class) {
                        params[i] = new JSONArray();
                        return;
                    }
                    if (types[i] == Integer.class) {
                        params[i] = 0;
                        return;
                    }
                    if (types[i] == JSONObject.class) {
                        params[i] = new JSONObject();
                        return;
                    }
                    if (types[i] == String.class) {
                        params[i] = "";
                        return;
                    }
                }
                throw new JsonTemplatingException(String.format("Function '%s' expected %s as %s parameter, but got %s", name, types[i]
                                .getTypeName(), StringUtils.getFormatterNumber(i + 1),
                        param == null ? "null" : param.getClass()), path);
            }
        }
    }

    public FunctionDefinition implementation(Function<Object[], Object> implementation, Class<?>... types) {
        implementations.add(implementation);
        this.types.add(types);
        return this;
    }

    public void disable() {
        Function<Object[], Object> disabled = objects -> {
            throw new JsonTemplatingException("This function has been disabled");
        };
        for (int i = 0; i < implementations.size(); i++) {
            implementations.set(i, disabled);
        }
    }

}
