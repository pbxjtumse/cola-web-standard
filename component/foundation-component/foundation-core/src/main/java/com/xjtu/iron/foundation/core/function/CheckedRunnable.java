package com.xjtu.iron.foundation.core.function;

/** 可抛出受检异常的无参操作。 */
@FunctionalInterface
public interface CheckedRunnable {
    void run() throws Exception;
}
