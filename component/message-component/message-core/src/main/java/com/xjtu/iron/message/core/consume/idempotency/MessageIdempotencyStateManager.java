package com.xjtu.iron.message.core.consume.idempotency;

/**
 * 幂等状态管理。
 *
 * <p>负责 acquire 和最终状态更新，不负责消费流程编排。</p>
 */
public interface MessageIdempotencyStateManager {

    IdempotentAcquireResult acquire(MessageIdempotencyContext context);

    void markSuccess(MessageIdempotencyContext context);

    void markDiscarded(MessageIdempotencyContext context);

    void markFailed(MessageIdempotencyContext context, Throwable throwable);
}
