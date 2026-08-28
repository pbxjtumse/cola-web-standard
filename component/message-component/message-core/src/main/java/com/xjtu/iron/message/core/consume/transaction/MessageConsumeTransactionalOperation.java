package com.xjtu.iron.message.core.consume.transaction;

/**
 * 可抛出运行时异常的事务回调。
 *
 * @param <T> 返回值类型
 */
@FunctionalInterface
public interface MessageConsumeTransactionalOperation<T> {
    T execute();
}
