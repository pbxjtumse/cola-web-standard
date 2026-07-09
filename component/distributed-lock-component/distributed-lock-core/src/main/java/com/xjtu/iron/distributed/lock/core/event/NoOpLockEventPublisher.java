package com.xjtu.iron.distributed.lock.core.event;

/**
 * 空事件发布器。
 */
public final class NoOpLockEventPublisher implements LockEventPublisher {

    @Override
    public void publish(LockEvent event) {
        // no-op
    }
}
