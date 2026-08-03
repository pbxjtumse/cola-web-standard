package com.xjtu.iron.foundation.core.function;

/**
 * 可抛出受检异常的函数式接口，供需要保留异常语义的模板类使用。
 */
@FunctionalInterface
public interface CheckedSupplier<T> {

    T get() throws Exception;
}
