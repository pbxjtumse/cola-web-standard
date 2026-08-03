package com.xjtu.iron.foundation.core.function;

/** 可抛出受检异常的单参数函数。 */
@FunctionalInterface
public interface CheckedFunction<T, R> {
    R apply(T value) throws Exception;
}
