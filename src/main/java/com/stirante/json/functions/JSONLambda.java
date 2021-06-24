package com.stirante.json.functions;

@FunctionalInterface
public interface JSONLambda {

    Object apply(Object[] args);

    default Object execute(Object... args) {
        return apply(args);
    }

    /**
     * Returns a function that always returns its input argument.
     *
     * @return a function that always returns its input argument
     */
    static JSONLambda identity() {
        return t -> t;
    }
}
