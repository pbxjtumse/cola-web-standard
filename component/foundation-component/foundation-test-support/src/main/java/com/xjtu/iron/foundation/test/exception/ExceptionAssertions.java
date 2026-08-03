package com.xjtu.iron.foundation.test.exception;

/**
 * 提供不绑定测试框架的异常断言。
 */
public final class ExceptionAssertions {

    private ExceptionAssertions() {
    }

    public static <T extends Throwable> T expect(Class<T> type, ThrowingAction action) {
        try {
            action.run();
        } catch (Throwable throwable) {
            if (type.isInstance(throwable)) {
                return type.cast(throwable);
            }
            throw new AssertionError("expected " + type.getName() + " but got " + throwable.getClass().getName(), throwable);
        }
        throw new AssertionError("expected exception " + type.getName() + " but nothing was thrown");
    }

    @FunctionalInterface
    public interface ThrowingAction {
        void run() throws Throwable;
    }
}
