package com.xjtu.iron.idempotent.core.observation;

/**
 * 幂等生命周期事件发布 SPI。
 *
 * <p>事件属于旁路观测能力。实现方必须确保 listener 失败不会反向破坏
 * Repository 状态转换或业务执行结果。</p>
 */
@FunctionalInterface
public interface IdempotencyEventPublisher {

    void publish(IdempotencyEvent event);

    static IdempotencyEventPublisher noop() {
        return event -> {
            // no-op
        };
    }
}
