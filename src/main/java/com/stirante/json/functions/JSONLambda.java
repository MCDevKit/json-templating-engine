package com.stirante.json.functions;

@FunctionalInterface
public interface JSONLambda {

    Object execute(Object... args);

    /**
     * Returns a function that always returns its input argument.
     *
     * @return a function that always returns its input argument
     */
    static JSONLambda identity() {
        return t -> t;
    }
}
