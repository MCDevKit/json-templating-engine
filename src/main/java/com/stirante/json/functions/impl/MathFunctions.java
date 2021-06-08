package com.stirante.json.functions.impl;

import com.stirante.json.functions.JSONFunction;
import org.json.JSONArray;

import java.util.Arrays;

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
    private static Number round(Number num, Number precision) {
        return Math.round(num.doubleValue() * Math.pow(10, precision.intValue())) / (Math.pow(10, precision.intValue()));
    }

    @JSONFunction
    private static Number sin(Number num) {
        return Math.sin(Math.toRadians(num.doubleValue()));
    }

    @JSONFunction
    private static Number cos(Number num) {
        return Math.cos(Math.toRadians(num.doubleValue()));
    }

    @JSONFunction
    private static Number tan(Number num) {
        return Math.tan(Math.toRadians(num.doubleValue()));
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

    @JSONFunction
    private static Number pi() {
        return Math.PI;
    }

    @JSONFunction
    private static JSONArray rotationToNormal(Number xRot, Number yRot) {
        Number x = round(cos(xRot).doubleValue() * sin(yRot).doubleValue(), 5);
        Number y = round(-sin(xRot).doubleValue(), 5);
        Number z = round(cos(yRot).doubleValue() * cos(xRot).doubleValue(), 5);

        return new JSONArray(Arrays.asList(x, y, z));
    }

}
