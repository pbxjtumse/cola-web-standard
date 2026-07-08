package com.xjtu.iron.distributed.lock.api;

/**
 * 锁等待策略。
 *
 * <p>用于描述加锁失败后是否等待以及如何等待。分布式锁不建议默认无限等待，业务应显式配置
 * waitTime 和等待策略，避免线程池被大量阻塞请求拖死。</p>
 */
public enum LockWaitStrategy {

    /**
     * 不等待。
     *
     * <p>只尝试加锁一次，失败立即返回 {@link LockStatus#NOT_ACQUIRED}。适合定时任务抢占、缓存重建、
     * 批处理分片等“抢到就执行，抢不到就跳过”的场景。</p>
     */
    NO_WAIT,

    /**
     * 指数退避 + jitter。
     *
     * <p>加锁失败后按照退避策略短暂睡眠，然后继续尝试，直到成功或超过 waitTime。适合用户请求、
     * 短事务保护、轻微热点竞争场景。</p>
     */
    BACKOFF,

    /**
     * 发布订阅通知 + 本地退避兜底。
     *
     * <p>加锁失败后订阅释放通知，收到通知后重试，同时保留本地退避重试兜底，避免通知丢失导致永久等待。
     * Redis Provider 二期支持；一期可以先保留枚举但不启用。</p>
     */
    PUBSUB_BACKOFF
}
