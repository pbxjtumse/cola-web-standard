package com.xjtu.iron.idempotent.api.context;

import com.xjtu.iron.idempotent.api.policy.IdempotencyMode;

import java.time.Instant;

/**
 * 业务 callback 获取到的当前幂等执行权上下文。
 *
 * <p>只有真正抢到 PROCESSING 执行权的调用才会拿到该对象。
 * 这里最关键的是 {@code ownerToken + version}：它们共同标识当前 generation。
 * 当 PROCESSING 超时并被恢复任务接管后，新的 generation 会得到新的 ownerToken 和更大的 version。</p>
 */
public final class IdempotencyContext {

    /** 业务隔离域，例如 order / payment。 */
    private final String namespace;

    /** 逻辑幂等键，例如 create-order:REQ-10001。 */
    private final String key;

    /** 分片路由元数据；当前组件只传递，不负责计算分库分表规则。 */
    private final String routeKey;

    /** 当前 generation 的执行者身份。 */
    private final String ownerToken;

    /** 当前 generation 版本；每次显式恢复/新 generation 都应单调增加。 */
    private final long version;

    /** WINDOWED 或 DURABLE。 */
    private final IdempotencyMode mode;

    /** true 表示本次 callback 来自 recover()，而不是普通 execute()。 */
    private final boolean recoveryExecution;

    /** 当前 generation 被抢占成功的大致时间。 */
    private final Instant acquiredAt;

    /** 当前 PROCESSING 执行权的逻辑过期时间。 */
    private final Instant processingExpireAt;

    public IdempotencyContext(
            String namespace,
            String key,
            String routeKey,
            String ownerToken,
            long version,
            IdempotencyMode mode,
            boolean recoveryExecution,
            Instant acquiredAt,
            Instant processingExpireAt) {
        this.namespace = namespace;
        this.key = key;
        this.routeKey = routeKey;
        this.ownerToken = ownerToken;
        this.version = version;
        this.mode = mode;
        this.recoveryExecution = recoveryExecution;
        this.acquiredAt = acquiredAt;
        this.processingExpireAt = processingExpireAt;
    }

    public String getNamespace() { return namespace; }
    public String getKey() { return key; }
    public String getRouteKey() { return routeKey; }
    public String getOwnerToken() { return ownerToken; }
    public long getVersion() { return version; }

    /**
     * 当前幂等 generation 的版本号。
     *
     * <p>它用于幂等记录自身的 owner/version CAS。不要默认把它等同于
     * distributed-lock 的 fencingToken：WINDOWED 记录在物理删除后可能从 version=1
     * 重新开始，因此它不天然具备“跨记录生命周期永远单调递增”的 fencing 保证。</p>
     */
    public long getGenerationVersion() { return version; }

    /**
     * V1.2 兼容名称。
     *
     * @deprecated 该名字容易让业务误以为它等价于分布式锁的全生命周期 fencingToken。
     *             请使用 {@link #getGenerationVersion()}，若业务资源需要真正 fencing，
     *             应显式设计单调 token 的权威来源与条件写协议。
     */
    @Deprecated
    public long fencingVersion() { return version; }

    public IdempotencyMode getMode() { return mode; }
    public boolean isRecoveryExecution() { return recoveryExecution; }
    public Instant getAcquiredAt() { return acquiredAt; }
    public Instant getProcessingExpireAt() { return processingExpireAt; }
}
