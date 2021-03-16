package com.stirante.json.functions;

import com.stirante.json.JsonProcessor;

public class MathFunctions {

    public static void register() {
        JsonProcessor.defineFunction(new JsonProcessor.FunctionDefinition("floor")
                .implementation(MathFunctions::floor, Number.class)
        );
        JsonProcessor.defineFunction(new JsonProcessor.FunctionDefinition("ceil")
                .implementation(MathFunctions::ceil, Number.class)
        );
        JsonProcessor.defineFunction(new JsonProcessor.FunctionDefinition("mod")
                .implementation(MathFunctions::mod, Number.class, Number.class)
        );
    }

    private static Object floor(Object[] params) {
        return (int) Math.floor(((Number) params[0]).doubleValue());
    }

    private static Object ceil(Object[] params) {
        return (int) Math.floor(((Number) params[0]).doubleValue());
    }

    private static Object mod(Object[] params) {
        return ((Number) params[0]).doubleValue() % ((Number) params[1]).doubleValue();
    }

}
