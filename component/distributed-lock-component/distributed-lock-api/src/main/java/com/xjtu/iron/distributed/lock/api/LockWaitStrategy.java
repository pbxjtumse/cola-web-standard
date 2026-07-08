package com.xjtu.iron.distributed.lock.api;

public enum LockWaitStrategy {

    /**
     * 不等待，抢不到立即返回。
     */
    NO_WAIT,

    /**
     * 指数退避 + jitter。
     */
    BACKOFF,

    /**
     * Redis pub/sub 通知 + 本地退避兜底。
     */
    PUBSUB_BACKOFF
}
