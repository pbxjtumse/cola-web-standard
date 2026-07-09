package com.xjtu.iron.distributed.lock.core.event;

/**
 * 分布式锁事件发布器。
 */
public interface LockEventPublisher {

    /**
     * 发布锁事件。
     *
     * @param event 锁事件。
     */
    void publish(LockEvent event);
}
