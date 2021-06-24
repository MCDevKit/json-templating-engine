package com.stirante.json.functions;

import com.stirante.json.exception.JsonTemplatingException;
import com.stirante.json.utils.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FunctionDefinition {

    private static final Map<Class<?>, String> NAME_MAP = new HashMap<>();

    static {
        NAME_MAP.put(JSONObject.class, "<Object>");
        NAME_MAP.put(JSONArray.class, "<Array>");
        NAME_MAP.put(Number.class, "<Number>");
        NAME_MAP.put(Integer.class, "<Integer>");
        NAME_MAP.put(Double.class, "<Double>");
        NAME_MAP.put(Float.class, "<Float>");
        NAME_MAP.put(Boolean.class, "<Boolean>");
        NAME_MAP.put(String.class, "<String>");
        NAME_MAP.put(Long.class, "<Long>");
        NAME_MAP.put(JSONLambda.class, "<Lambda>");
    }

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
        List<Pair<Integer, Class<?>[]>> sizeMatching = IntStream.range(0, types.size())
                .filter(value -> params.length == types.get(value).length)
                .mapToObj(value -> new Pair<>(value, types.get(value)))
                .collect(Collectors.toList());
        if (sizeMatching.size() == 0) {
            throw new JsonTemplatingException(String.format("Incorrect number of parameters passed to function '%s'!", name), path);
        }
        List<Pair<Integer, Class<?>[]>> matching =
                sizeMatching.stream()
                        .filter(classes -> paramCheck(params, classes.getValue()))
                        .collect(Collectors.toList());
        if (matching.size() == 0) {
            throw new JsonTemplatingException(String.format("Function '%s' got unexpected params. Expected %s, but got %s", name, sizeMatching
                    .stream()
                    .map(integerPair -> toString(integerPair.getValue()))
                    .collect(Collectors.joining(", ")),
                    toString(params)), path);
        }
        else if (matching.size() == 1) {
            Pair<Integer, Class<?>[]> pair = matching.get(0);
            for (int i = 0, paramsLength = params.length; i < paramsLength; i++) {
                params[i] = getEmptyIfNull(pair.getValue()[i], params[i]);
            }
            try {
                return implementations.get(pair.getKey()).apply(params);
            } catch (JsonTemplatingException ex) {
                throw ex.withPath(path);
            }
        }
        else {
            throw new JsonTemplatingException(String.format("Ambiguous function call to '%s'. Following variants matched: %s", name, matching
                    .stream()
                    .map(integerPair -> toString(integerPair.getValue()))
                    .collect(Collectors.joining(", "))), path);
        }
    }

    private String toString(Class<?>[] classes) {
        return name + "(" + Arrays.stream(classes).map(NAME_MAP::get).collect(Collectors.joining(", ")) + ")";
    }

    private String toString(Object[] params) {
        return "(" + Arrays.stream(params).map(Object::getClass).map(key -> NAME_MAP.getOrDefault(key, key.getName())).collect(Collectors.joining(", ")) + ")";
    }

    @SuppressWarnings("unchecked")
    private <T> T getEmptyIfNull(Class<T> cls, Object param) {
        // Convert Lists to JSONArrays and Maps to JSONObjects
        if (cls == JSONArray.class && param instanceof List) {
            return (T) new JSONArray((List<?>)param);
        }
        if (cls == JSONObject.class && param instanceof Map) {
            return (T) new JSONObject((Map<?, ?>) param);
        }
        if (!cls.isInstance(param)) {
            if (cls == JSONArray.class) {
                return (T) new JSONArray();
            }
            else if (cls == Integer.class) {
                return (T) Integer.valueOf(0);
            }
            else if (cls == Double.class) {
                return (T) Double.valueOf(0);
            }
            else if (cls == Float.class) {
                return (T) Float.valueOf(0);
            }
            else if (cls == Long.class) {
                return (T) Long.valueOf(0);
            }
            else if (cls == Number.class) {
                return (T) Integer.valueOf(0);
            }
            else if (cls == JSONObject.class) {
                return (T) new JSONObject();
            }
            else if (cls == String.class) {
                return (T) "";
            }
            else if (cls == Boolean.class) {
                return (T) Boolean.FALSE;
            }
            else if (cls == JSONLambda.class) {
                return (T) JSONLambda.identity();
            }
            throw new IllegalArgumentException("Unexpected type: " + cls.getName());
        }
        else {
            return (T) param;
        }
    }

    private boolean paramCheck(Object[] params, Class<?>[] types) {
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            if (types[i] == JSONArray.class && param instanceof List) return true;
            if (types[i] == JSONObject.class && param instanceof Map) return true;
            if (!types[i].isInstance(param) && param != null) {
                return false;
            }
        }
        return true;
    }

    public void addImplementation(Function<Object[], Object> implementation, Class<?>... types) {
        implementations.add(implementation);
        this.types.add(types);
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
