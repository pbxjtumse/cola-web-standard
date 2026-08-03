package com.xjtu.iron.foundation.core.validation;

/**
 * 对象状态校验工具。
 */
public final class States {

    private States() {
    }

    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new IllegalStateException(message);
        }
    }

    public static void isFalse(boolean expression, String message) {
        if (expression) {
            throw new IllegalStateException(message);
        }
    }
}
