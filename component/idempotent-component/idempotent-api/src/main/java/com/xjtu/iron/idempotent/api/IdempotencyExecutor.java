package com.xjtu.iron.idempotent.api;

import java.util.Objects;

/**
 * 幂等业务统一入口，不直接依赖 Redis/JDBC/Redisson。
 *
 * <p>V1.1 明确拆分：</p>
 * <ul>
 *     <li>{@link #execute}：普通 API / 消息消费路径，不自动接管超时 PROCESSING；</li>
 *     <li>{@link #recover}：Reliable Task 恢复路径，仅对 recoveryMode=EXTERNAL_TASK 的记录生效。</li>
 * </ul>
 */
public interface IdempotencyExecutor {

    <T> IdempotencyResult<T> execute(
            IdempotencyRequest request,
            Class<T> resultType,
            IdempotencyCallback<T> callback);

    <T> IdempotencyResult<T> recover(
            IdempotencyRecoveryRequest request,
            Class<T> resultType,
            IdempotencyCallback<T> callback);

    default <T> IdempotencyResult<T> execute(
            String key,
            IdempotencyOptions options,
            Class<T> type,
            IdempotencyCallback<T> callback) {
        Objects.requireNonNull(callback, "callback must not be null");
        return execute(IdempotencyRequest.of(key, options), type, callback);
    }

    default <T> IdempotencyResult<T> execute(
            String key,
            IdempotencyOptions options,
            IdempotencyCallback<T> callback) {
        return execute(IdempotencyRequest.of(key, options), null, callback);
    }
}
