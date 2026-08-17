package com.xjtu.iron.idempotent.api.execution;

import com.xjtu.iron.idempotent.api.context.IdempotencyContext;

/**
 * 真正业务逻辑的回调接口。
 *
 * <p>只有 Repository 返回 ACQUIRED / RECOVERY_ACQUIRED 时才会进入 callback。
 * 对于 REPLAYED、PROCESSING、FAILED、KEY_CONFLICT 等结果，业务不会再次执行。</p>
 *
 * <p>{@link IdempotencyContext} 会携带当前 generation 的 ownerToken/version。
 * 对高风险写操作，可以把 version 继续传递给业务表做条件更新，从而拒绝旧执行者。</p>
 *
 * @param <T> 业务返回值类型
 */
@FunctionalInterface
public interface IdempotencyCallback<T> {

    T doWithIdempotency(IdempotencyContext context) throws Exception;
}
