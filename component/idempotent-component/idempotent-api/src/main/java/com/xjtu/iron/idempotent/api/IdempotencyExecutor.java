package com.xjtu.iron.idempotent.api;

import java.util.Objects;

/**
 * 业务统一入口，不直接依赖 Redis/JDBC/Redisson。
 */
public interface IdempotencyExecutor {
    <T> IdempotencyResult<T> execute(IdempotencyRequest request, Class<T> resultType, IdempotencyCallback<T> callback);

    default <T> IdempotencyResult<T> execute(String key, IdempotencyOptions options, Class<T> type, IdempotencyCallback<T> callback) {
        Objects.requireNonNull(callback);
        return execute(IdempotencyRequest.of(key, options), type, callback);
    }

    default <T> IdempotencyResult<T> execute(String key, IdempotencyOptions options, IdempotencyCallback<T> callback) {
        return execute(IdempotencyRequest.of(key, options), null, callback);
    }
}
