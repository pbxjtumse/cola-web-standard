package com.xjtu.iron.foundation.core.function;

/** 可抛出受检异常的双参数函数。 */
@FunctionalInterface
public interface CheckedBiFunction<T, U, R> {
    R apply(T left, U right) throws Exception;
}
