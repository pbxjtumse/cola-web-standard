package com.xjtu.iron.foundation.core.function;

/** 可抛出受检异常的单参数消费者。 */
@FunctionalInterface
public interface CheckedConsumer<T> {
    void accept(T value) throws Exception;
}
