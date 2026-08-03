package com.xjtu.iron.foundation.core.function;

/** 可抛出受检异常的值提供者。 */
@FunctionalInterface
public interface CheckedSupplier<T> {
    T get() throws Exception;
}
