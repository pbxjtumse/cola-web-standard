package com.xjtu.iron.distributed.lock.core;

import com.xjtu.iron.distributed.lock.api.LockStage;
import com.xjtu.iron.distributed.lock.api.LockStatus;
import com.xjtu.iron.distributed.lock.api.exception.LockLostException;
import com.xjtu.iron.distributed.lock.core.event.LockEvent;
import com.xjtu.iron.distributed.lock.core.event.LockEventPublisher;
import com.xjtu.iron.distributed.lock.core.event.LockEventType;
import com.xjtu.iron.distributed.lock.core.metrics.LockMetricsRecorder;
import com.xjtu.iron.distributed.lock.core.name.LockNamePatternResolver;
import com.xjtu.iron.distributed.lock.core.runtime.LockRuntimeState;
import com.xjtu.iron.distributed.lock.core.spi.request.LockCheckRequest;
import com.xjtu.iron.distributed.lock.core.spi.response.LockCheckResponse;
import com.xjtu.iron.distributed.lock.core.spi.model.LockLease;
import com.xjtu.iron.distributed.lock.core.spi.LockProvider;
import com.xjtu.iron.distributed.lock.core.spi.request.LockReleaseRequest;
import com.xjtu.iron.distributed.lock.core.spi.response.LockReleaseResponse;
import com.xjtu.iron.distributed.lock.core.spi.request.LockRenewRequest;
import com.xjtu.iron.distributed.lock.core.spi.response.LockRenewResponse;
import com.xjtu.iron.distributed.lock.core.watchdog.WatchdogLockHandle;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * 默认锁句柄实现。
 *
 * <p>DefaultLockHandle 表示一次成功加锁后的本地租约句柄。它持有不可变的 {@link LockLease}，
 * 并通过 {@link LockRuntimeState} 保存 lost/releaseAttempted 这类运行态标记。</p>
 *
 * <p>注意：本类不使用 Java Thread 判断锁归属。所有续期、释放、检查操作都依赖 ownerToken。
 * 因此同一个 LockHandle 可以跨线程传递。</p>
 */
public final class DefaultLockHandle implements WatchdogLockHandle {

    /** 底层锁 Provider。 */
    private final LockProvider provider;

    /** 不可变租约数据。 */
    private final LockLease lease;

    /** 本地运行时状态。 */
    private final LockRuntimeState runtimeState;

    /** 事件发布器。 */
    private final LockEventPublisher eventPublisher;

    /** 指标记录器。 */
    private final LockMetricsRecorder metricsRecorder;

    /** 锁名称 pattern 解析器，用于降低指标 tag 基数。 */
    private final LockNamePatternResolver patternResolver;

    public DefaultLockHandle(
            LockProvider provider,
            LockLease lease,
            LockRuntimeState runtimeState,
            LockEventPublisher eventPublisher,
            LockMetricsRecorder metricsRecorder,
            LockNamePatternResolver patternResolver
    ) {
        this.provider = Objects.requireNonNull(provider, "provider must not be null");
        this.lease = Objects.requireNonNull(lease, "lease must not be null");
        this.runtimeState = Objects.requireNonNull(runtimeState, "runtimeState must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.metricsRecorder = Objects.requireNonNull(metricsRecorder, "metricsRecorder must not be null");
        this.patternResolver = Objects.requireNonNull(patternResolver, "patternResolver must not be null");
    }

    /**
     * 获取内部租约快照。
     *
     * <p>该方法仅供 core 内部编排使用，不建议暴露到 API 层。</p>
     */
    public LockLease lease() {
        return lease;
    }

    /**
     * 获取运行时状态。
     *
     * <p>该方法仅供 core 内部编排和单元测试使用。</p>
     */
    public LockRuntimeState runtimeState() {
        return runtimeState;
    }

    @Override
    public String lockName() {
        return lease.getLockName();
    }

    @Override
    public String lockKey() {
        return lease.getLockKey();
    }

    @Override
    public String ownerToken() {
        return lease.getOwnerToken();
    }

    @Override
    public OptionalLong fencingToken() {
        return lease.fencingToken();
    }

    @Override
    public Instant acquiredAt() {
        return lease.getAcquiredAt();
    }

    @Override
    public Duration leaseTime() {
        return lease.getLeaseTime();
    }

    @Override
    public Instant expireAt() {
        return lease.getExpireAt();
    }

    @Override
    public boolean isLost() {
        return runtimeState.isLost();
    }

    @Override
    public boolean isReleaseAttempted() {
        return runtimeState.isReleaseAttempted();
    }

    @Override
    public boolean checkHeld() {
        if (runtimeState.isReleaseAttempted() || runtimeState.isLost()) {
            return false;
        }
        LockCheckResponse response = provider.check(LockCheckRequest.fromLease(lease));
        switch (response.getStatus()) {
            case HELD:
                return true;
            case NOT_FOUND:
            case NOT_OWNER:
                markLost(LockStage.CHECK, null);
                return false;
            case PROVIDER_ERROR:
            default:
                publishProviderError(LockStage.CHECK, response.getError());
                return false;
        }
    }

    @Override
    public boolean renew() {
        if (runtimeState.isReleaseAttempted() || runtimeState.isLost()) {
            return false;
        }
        LockRenewResponse response = provider.renew(LockRenewRequest.fromLease(lease));
        switch (response.getStatus()) {
            case RENEWED:
                metricsRecorder.recordRenew(lease.getProviderName(), lease.getNamespace(), true);
                publish(LockEventType.RENEWED, LockStage.RENEW, null, null);
                return true;
            case NOT_FOUND:
            case NOT_OWNER:
                metricsRecorder.recordRenew(lease.getProviderName(), lease.getNamespace(), false);
                markLost(LockStage.RENEW, null);
                return false;
            case PROVIDER_ERROR:
            default:
                metricsRecorder.recordRenew(lease.getProviderName(), lease.getNamespace(), false);
                publishProviderError(LockStage.RENEW, response.getError());
                return false;
        }
    }

    @Override
    public boolean unlock() {
        return releaseWithResult().isReleased();
    }

    /**
     * 执行释放并返回 Provider 细粒度释放结果。
     *
     * <p>execute 模板需要知道 release 是 RELEASED、NOT_OWNER、NOT_FOUND 还是 PROVIDER_ERROR，
     * 因此这里提供比 {@link #unlock()} 更细的 core 内部方法。</p>
     */
    public LockReleaseResponse releaseWithResult() {
        if (!runtimeState.markReleaseAttemptedOnce()) {
            // 本地 release 流程已经执行过，说明这是重复 unlock/close/finally。
            // 这里不能返回 NOT_FOUND，否则 execute 模板可能把重复释放误判成失锁。
            // 返回 RELEASED 只表示“本次无需再执行远程释放”，不会代表这次调用真的删除了 Redis key。
            return LockReleaseResponse.released();
        }
        LockReleaseResponse response = provider.release(LockReleaseRequest.fromLease(lease));
        switch (response.getStatus()) {
            case RELEASED:
                metricsRecorder.recordRelease(lease.getProviderName(), lease.getNamespace(), true);
                publish(LockEventType.RELEASED, LockStage.RELEASE, null, null);
                return response;
            case NOT_FOUND:
            case NOT_OWNER:
                metricsRecorder.recordRelease(lease.getProviderName(), lease.getNamespace(), false);
                markLost(LockStage.RELEASE, null);
                return response;
            case PROVIDER_ERROR:
            default:
                metricsRecorder.recordRelease(lease.getProviderName(), lease.getNamespace(), false);
                publish(LockEventType.RELEASE_FAILED, LockStage.RELEASE, LockStatus.RELEASE_FAILED, response.getError());
                return response;
        }
    }

    @Override
    public void assertHeld() {
        if (!checkHeld()) {
            throw new LockLostException("lock lost: " + lease.getLockName());
        }
    }

    @Override
    public String watchdogId() {
        return lease.getProviderName() + ':' + lease.getLockKey() + ':' + lease.getOwnerToken();
    }

    @Override
    public void markLostByWatchdog(String reason, Throwable error) {
        if (runtimeState.markLostOnce()) {
            metricsRecorder.recordLost(lease.getProviderName(), lease.getNamespace());
            publish(LockEventType.LOCK_LOST, LockStage.RENEW, LockStatus.LOCK_LOST, error);
        }
    }

    private void markLost(LockStage stage, Throwable error) {
        if (runtimeState.markLostOnce()) {
            metricsRecorder.recordLost(lease.getProviderName(), lease.getNamespace());
            publish(LockEventType.LOCK_LOST, stage, LockStatus.LOCK_LOST, error);
        }
    }

    private void publishProviderError(LockStage stage, Throwable error) {
        publish(LockEventType.PROVIDER_ERROR, stage, LockStatus.PROVIDER_ERROR, error);
    }

    private void publish(LockEventType type, LockStage stage, LockStatus status, Throwable error) {
        eventPublisher.publish(LockEvent.builder()
                .eventType(type)
                .stage(stage)
                .status(status)
                .namespace(lease.getNamespace())
                .lockName(lease.getLockName())
                .lockKey(lease.getLockKey())
                .providerName(lease.getProviderName())
                .ownerToken(lease.getOwnerToken())
                .fencingToken(lease.fencingToken().isPresent() ? lease.fencingToken().getAsLong() : null)
                .error(error)
                .build());
    }

    @SuppressWarnings("unused")
    private String lockNamePattern() {
        return patternResolver.resolvePattern(lease.getLockName());
    }
}
