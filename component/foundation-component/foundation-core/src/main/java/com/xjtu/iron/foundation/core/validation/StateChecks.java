package com.xjtu.iron.foundation.core.validation;

/**
 * 校验对象运行状态并抛出 {@link IllegalStateException}。
 */
public final class StateChecks {

    private StateChecks() {
    }

    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new IllegalStateException(message);
        }
    }

    public static <T> T notNull(T value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
        return value;
    }
}
