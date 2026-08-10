package com.xjtu.iron.distributed.lock.api;

/**
 * 锁等待策略。
 *
 * <p>这里只描述 Core 与 Provider 的等待职责，不绑定某个具体产品。这样未来 Redisson 的 Pub/Sub、
 * ZooKeeper 的 watch、Etcd 的 watch 都可以映射到同一个 {@link #PROVIDER_NATIVE} 语义。</p>
 */
public enum LockWaitStrategy {

    /** 不等待，抢不到立即返回。 */
    NO_WAIT,

    /** 由 iron-lock Core 使用退避 + jitter 周期性重新 acquire。 */
    BACKOFF,

    /**
     * 由 Provider 自己完成等待与唤醒。
     *
     * <p>当前 Redisson 使用 RLock/RFencedLock 的原生 Pub/Sub 等待；未来 ZooKeeper/Etcd 可以使用
     * watch 机制。Core 只调用 Provider.acquire() 一次，并把完整 waitTime 交给 Provider。</p>
     */
    PROVIDER_NATIVE
}
