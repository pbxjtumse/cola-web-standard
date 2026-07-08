package com.xjtu.iron.distributed.lock.core.spi;

import java.time.Duration;

/**
 * 分布式锁底层实现 SPI。
 *
 * <p>Provider 负责对接具体存储或协调系统，例如 Redis、Redisson、Zookeeper、Etcd、JDBC。
 * 上层 {@code DistributedLockClient} 不直接感知具体实现，只依赖本接口完成加锁、释放、续期和持锁检查。</p>
 *
 * <p>Provider 只负责底层原子语义，不负责业务执行模板、指标、事件、等待策略和 watchdog 调度。
 * 这些通用能力应放在 core 层。</p>
 */
public interface LockProvider {

    /**
     * Provider 名称。
     *
     * <p>例如 {@code redis}、{@code redisson}、{@code zookeeper}、{@code etcd}、{@code jdbc}。</p>
     *
     * @return Provider 名称。
     */
    String providerName();

    /**
     * 尝试获取锁。
     *
     * <p>该方法只做一次底层加锁尝试，不应该在 Provider 内部做长时间等待。等待策略由 core 层的 LockWaiter 负责。</p>
     *
     * @param request 加锁请求。
     * @return 加锁响应。
     */
    LockAcquireResponse acquire(LockAcquireRequest request);

    /**
     * 安全释放锁。
     *
     * <p>Provider 必须校验 {@link LockLease#getOwnerToken()}，只有底层锁仍然属于当前 ownerToken 时才允许释放。</p>
     *
     * @param lease 锁租约。
     * @return 释放响应。
     */
    LockReleaseResponse release(LockLease lease);

    /**
     * 安全续期锁。
     *
     * <p>Provider 必须校验 ownerToken，只有底层锁仍然属于当前 ownerToken 时才允许刷新 TTL。</p>
     *
     * @param lease     锁租约。
     * @param leaseTime 新租约时间。
     * @return 续期响应。
     */
    LockRenewResponse renew(LockLease lease, Duration leaseTime);

    /**
     * 检查当前租约是否仍然持有锁。
     *
     * @param lease 锁租约。
     * @return 当前 ownerToken 仍然持有锁返回 {@code true}。
     */
    boolean isHeld(LockLease lease);

    /**
     * Provider 能力描述。
     *
     * @return Provider 能力。
     */
    LockProviderCapabilities capabilities();
}
