package com.xjtu.iron.distributed.lock.api;

/**
 * 锁等待策略。
 *
 * <p>等待策略用于描述“第一次没有抢到锁之后怎么等”。组件不建议提供无限等待能力，业务应显式设置
 * waitTime，超时后返回 NOT_ACQUIRED。</p>
 */
public enum LockWaitStrategy {

    /** 不等待，抢不到立即返回。适合定时任务抢占、缓存重建等场景。 */
    NO_WAIT,

    /** 指数退避 + jitter。适合短等待、轻微竞争的用户请求或批处理抢占。 */
    BACKOFF,

    /** Pub/Sub 释放通知 + 本地退避兜底。适合中长等待，通常二期再实现。
     * 适用场景如下 ：
     *  1. 锁竞争比较激烈；
     *  2. waitTime 比较长，例如 3 秒、5 秒、10 秒；
     *  3. 不希望大量线程一直轮询 Redis；
     *  4. unlock 时可以发布释放通知；
     *  5. 等待方可以订阅 release channel，被通知后再尝试抢锁。
     * 处理场景较为复杂 需要注意
     *  1. 订阅刚建立前，锁已经释放了怎么办？
     *  2. pub/sub 消息丢了怎么办？
     *  3. Redis 连接断开怎么办？
     *  4. 多个等待者同时收到通知，惊群怎么办？
     *  5. channel 管理在哪里做？
     *  6. 订阅线程池如何管理？
     *  7. Redis Cluster 下 channel 和 key 如何设计？
     * */
    PUBSUB_BACKOFF
}
