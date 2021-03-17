package com.stirante.json.functions;

public class MathFunctions {

    @JSONFunction
    private static Integer floor(Number num) {
        return (int) Math.floor(num.doubleValue());
    }

    @JSONFunction
    private static Integer ceil(Number num) {
        return (int) Math.ceil(num.doubleValue());
    }

    @JSONFunction
    private static Integer round(Number num) {
        return (int) Math.round(num.doubleValue());
    }

    @JSONFunction
    private static Number abs(Number num) {
        return Math.abs(num.doubleValue());
    }

    @JSONFunction
    private static Number clamp(Number num, Number min, Number max) {
        return Math.max(Math.min(max.doubleValue(), num.doubleValue()), min.doubleValue());
    }

    @JSONFunction
    private static Number min(Number a, Number b) {
        return Math.min(a.doubleValue(), b.doubleValue());
    }

    @JSONFunction
    private static Number max(Number a, Number b) {
        return Math.max(a.doubleValue(), b.doubleValue());
    }

    @JSONFunction
    private static Number mod(Number a, Number b) {
        return a.doubleValue() % b.doubleValue();
    }

}
