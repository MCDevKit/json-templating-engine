package com.glowfischdesignstudio.jsonte.functions.impl;

import com.glowfischdesignstudio.jsonte.functions.JSONFunction;
import org.json.JSONArray;

import java.util.Arrays;

/**
 * Math functions allow for executing more complicated arithmetic.
 */
public class MathFunctions {

    /**
     * Returns an integer number rounded down.
     * @param num number: A number to floor
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be 3",
     *     "test": "{{floor(3.6)}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    private static Integer floor(Number num) {
        return (int) Math.floor(num.doubleValue());
    }

    /**
     * Returns an integer number rounded up.
     * @param num number: A number to ceil
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be 4",
     *     "test": "{{ceil(3.3)}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    private static Integer ceil(Number num) {
        return (int) Math.ceil(num.doubleValue());
    }

    @JSONFunction
    private static Integer round(Number num) {
        return (int) Math.round(num.doubleValue());
    }

    /**
     * Returns the nearest number with specified precision.
     * @param num number: A number to round
     * @param precision precision: A number of decimal places. If none specified, it will round to the nearest integer.
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be 3.142",
     *     "test": "{{round(3.1415, 3)}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    private static Number round(Number num, Number precision) {
        return Math.round(num.doubleValue() * Math.pow(10, precision.intValue())) / (Math.pow(10, precision.intValue()));
    }

    /**
     * Returns the trigonometric sine of an angle in degrees.
     * @param num number: An angle in degrees
     */
    @JSONFunction
    private static Number sin(Number num) {
        return Math.sin(Math.toRadians(num.doubleValue()));
    }

    /**
     * Returns the trigonometric cosine of an angle in degrees.
     * @param num number: An angle in degrees
     */
    @JSONFunction
    private static Number cos(Number num) {
        return Math.cos(Math.toRadians(num.doubleValue()));
    }

    /**
     * Returns the trigonometric tangent of an angle in degrees.
     * @param num number: An angle in degrees
     */
    @JSONFunction
    private static Number tan(Number num) {
        return Math.tan(Math.toRadians(num.doubleValue()));
    }

    /**
     * Returns the absolute value of a number.
     * @param num number: A number
     */
    @JSONFunction
    private static Number abs(Number num) {
        return Math.abs(num.doubleValue());
    }

    /**
     * Returns the specified number if it’s within the specified range. If not, it will return range start or end.
     * @param num number: A number
     * @param min min: Range start
     * @param max max: Range end
     */
    @JSONFunction
    private static Number clamp(Number num, Number min, Number max) {
        return Math.max(Math.min(max.doubleValue(), num.doubleValue()), min.doubleValue());
    }

    /**
     * Returns the smaller of two numbers.
     * @param a a: Number a
     * @param b b: Number b
     */
    @JSONFunction
    private static Number min(Number a, Number b) {
        return Math.min(a.doubleValue(), b.doubleValue());
    }

    /**
     * Returns the larger of two numbers.
     * @param a a: Number a
     * @param b b: Number b
     */
    @JSONFunction
    private static Number max(Number a, Number b) {
        return Math.max(a.doubleValue(), b.doubleValue());
    }

    /**
     * Returns the remainder (modulo) of the two arguments.
     * @param a number: A number
     * @param b denominator: A denominator
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be 1",
     *     "test": "{{mod(5, 2)}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    private static Number mod(Number a, Number b) {
        return a.doubleValue() % b.doubleValue();
    }

    /**
     * Returns the closest number to pi.
     */
    @JSONFunction
    private static Number pi() {
        return Math.PI;
    }

    /**
     * Returns normal (direction) vector based on pitch and yaw rotation.
     * @param xRot pitch: A pitch rotation (rotation in x-axis)
     * @param yRot yaw: A yaw rotation (rotation in y-axis)
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be [0,0,1]",
     *     "test": "{{rotationToNormal(0, 0)}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    private static JSONArray rotationToNormal(Number xRot, Number yRot) {
        Number x = round(cos(xRot).doubleValue() * sin(yRot).doubleValue(), 5);
        Number y = round(-sin(xRot).doubleValue(), 5);
        Number z = round(cos(yRot).doubleValue() * cos(xRot).doubleValue(), 5);

        return new JSONArray(Arrays.asList(x, y, z));
    }

    /**
     * Performs bitwise AND operation on two numbers.
     * @param a a: First number
     * @param b b: Second number
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be 1",
     *     "test": "{{bitwiseAnd(5, 3)}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    private static Number bitwiseAnd(Number a, Number b) {
        return a.intValue() & b.intValue();
    }

    /**
     * Performs bitwise OR operation on two numbers.
     * @param a a: First number
     * @param b b: Second number
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be 7",
     *     "test": "{{bitwiseOr(5, 3)}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    private static Number bitwiseOr(Number a, Number b) {
        return a.intValue() | b.intValue();
    }

    /**
     * Performs bitwise XOR (exclusive OR) operation on two numbers.
     * @param a a: First number
     * @param b b: Second number
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be 6",
     *     "test": "{{bitwiseXor(5, 3)}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    private static Number bitwiseXor(Number a, Number b) {
        return a.intValue() ^ b.intValue();
    }

    /**
     * Performs bitwise NOT operation on a number.
     * @param a a: A number
     * @example
     * <code>
     * {
     *   "$template": {
     *     "$comment": "The field below will be -6",
     *     "test": "{{bitwiseNot(5)}}"
     *   }
     * }
     * </code>
     */
    @JSONFunction
    private static Number bitwiseNot(Number a) {
        return ~a.intValue();
    }

}
