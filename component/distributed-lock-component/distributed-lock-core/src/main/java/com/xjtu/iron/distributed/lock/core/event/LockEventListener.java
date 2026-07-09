package com.xjtu.iron.distributed.lock.core.event;

/**
 * 分布式锁事件监听器。
 */
@FunctionalInterface
public interface LockEventListener {

    /**
     * 处理锁事件。
     *
     * @param event 锁事件。
     */
    void onEvent(LockEvent event);
}
