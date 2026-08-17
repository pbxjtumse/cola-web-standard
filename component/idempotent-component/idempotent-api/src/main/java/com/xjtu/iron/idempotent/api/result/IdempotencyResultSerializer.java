package com.xjtu.iron.idempotent.api.result;

/**
 * SNAPSHOT 结果策略使用的类型安全序列化器。
 */
public interface IdempotencyResultSerializer<T> {

    String serialize(T value) throws Exception;

    T deserialize(String payload) throws Exception;
}
