package com.xjtu.iron.distributed.lock.core;

import com.xjtu.iron.distributed.lock.api.LockStage;
import com.xjtu.iron.distributed.lock.api.LockStatus;
import com.xjtu.iron.distributed.lock.api.exception.LockLostException;
import com.xjtu.iron.distributed.lock.core.event.LockEventFactory;
import com.xjtu.iron.distributed.lock.core.event.LockEventPublisher;
import com.xjtu.iron.distributed.lock.core.event.LockEventType;
import com.xjtu.iron.distributed.lock.core.metrics.LockMetricsFacade;
import com.xjtu.iron.distributed.lock.core.result.LockReleaseOutcome;
import com.xjtu.iron.distributed.lock.core.runtime.LockRuntimeState;
import com.xjtu.iron.distributed.lock.core.spi.LockProvider;
import com.xjtu.iron.distributed.lock.core.spi.model.LockLease;
import com.xjtu.iron.distributed.lock.core.spi.request.LockCheckRequest;
import com.xjtu.iron.distributed.lock.core.spi.request.LockReleaseRequest;
import com.xjtu.iron.distributed.lock.core.spi.request.LockRenewRequest;
import com.xjtu.iron.distributed.lock.core.spi.response.LockCheckResponse;
import com.xjtu.iron.distributed.lock.core.spi.response.LockReleaseResponse;
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

    private final LockProvider provider;
    private final LockLease lease;
    private final LockRuntimeState runtimeState;
    private final LockEventPublisher eventPublisher;
    private final LockEventFactory eventFactory;
    private final LockMetricsFacade metricsFacade;

    public DefaultLockHandle(
            LockProvider provider,
            LockLease lease,
            LockRuntimeState runtimeState,
            LockEventPublisher eventPublisher,
            LockEventFactory eventFactory,
            LockMetricsFacade metricsFacade
    ) {
        this.provider = Objects.requireNonNull(provider, "provider must not be null");
        this.lease = Objects.requireNonNull(lease, "lease must not be null");
        this.runtimeState = Objects.requireNonNull(runtimeState, "runtimeState must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.eventFactory = Objects.requireNonNull(eventFactory, "eventFactory must not be null");
        this.metricsFacade = Objects.requireNonNull(metricsFacade, "metricsFacade must not be null");
    }

    public LockLease lease() { return lease; }
    public LockRuntimeState runtimeState() { return runtimeState; }
    @Override public String lockName() { return lease.getLockName(); }
    @Override public String lockKey() { return lease.getLockKey(); }
    @Override public String ownerToken() { return lease.getOwnerToken(); }
    @Override public OptionalLong fencingToken() { return lease.fencingToken(); }
    @Override public Instant acquiredAt() { return lease.getAcquiredAt(); }
    @Override public Duration leaseTime() { return lease.getLeaseTime(); }
    @Override public Instant expireAt() { return lease.getExpireAt(); }
    @Override public boolean isLost() { return runtimeState.isLost(); }
    @Override public boolean isReleaseAttempted() { return runtimeState.isReleaseAttempted(); }

    @Override
    public boolean checkHeld() {
        if (runtimeState.isReleaseAttempted() || runtimeState.isLost()) { return false; }
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
        if (runtimeState.isReleaseAttempted() || runtimeState.isLost()) { return false; }
        LockRenewResponse response = provider.renew(LockRenewRequest.fromLease(lease));
        switch (response.getStatus()) {
            case RENEWED:
                metricsFacade.recordRenew(lease, true);
                publish(LockEventType.RENEWED, LockStage.RENEW, null, null);
                return true;
            case NOT_FOUND:
            case NOT_OWNER:
                metricsFacade.recordRenew(lease, false);
                markLost(LockStage.RENEW, null);
                return false;
            case PROVIDER_ERROR:
            default:
                metricsFacade.recordRenew(lease, false);
                publishProviderError(LockStage.RENEW, response.getError());
                return false;
        }
    }

    @Override
    public boolean unlock() {
        return releaseWithOutcome().isReleased();
    }

    /** 执行释放并返回 core 内部释放解释结果。 */
    public LockReleaseOutcome releaseWithOutcome() {
        if (!runtimeState.markReleaseAttemptedOnce()) {
            return LockReleaseOutcome.alreadyAttempted();
        }
        LockReleaseResponse response = provider.release(LockReleaseRequest.fromLease(lease));
        LockReleaseOutcome outcome = LockReleaseOutcome.fromProviderResponse(response);
        switch (outcome.getType()) {
            case RELEASED:
                metricsFacade.recordRelease(lease, true);
                publish(LockEventType.RELEASED, LockStage.RELEASE, null, null);
                break;
            case LOCK_LOST:
                metricsFacade.recordRelease(lease, false);
                markLost(LockStage.RELEASE, null);
                break;
            case RELEASE_FAILED:
                metricsFacade.recordRelease(lease, false);
                publish(LockEventType.RELEASE_FAILED, LockStage.RELEASE, LockStatus.RELEASE_FAILED, outcome.getError());
                break;
            case ALREADY_ATTEMPTED:
            default:
                break;
        }
        return outcome;
    }

    @Override
    public void assertHeld() {
        if (!checkHeld()) { throw new LockLostException("lock lost: " + lease.getLockName()); }
    }

    @Override
    public String watchdogId() {
        return lease.getProviderName() + ':' + lease.getLockKey() + ':' + lease.getOwnerToken();
    }

    @Override
    public void markLostByWatchdog(String reason, Throwable error) {
        if (runtimeState.markLostOnce()) {
            metricsFacade.recordLost(lease);
            publish(LockEventType.LOCK_LOST, LockStage.RENEW, LockStatus.LOCK_LOST, error);
        }
    }

    private void markLost(LockStage stage, Throwable error) {
        if (runtimeState.markLostOnce()) {
            metricsFacade.recordLost(lease);
            publish(LockEventType.LOCK_LOST, stage, LockStatus.LOCK_LOST, error);
        }
    }

    private void publishProviderError(LockStage stage, Throwable error) {
        publish(LockEventType.PROVIDER_ERROR, stage, LockStatus.PROVIDER_ERROR, error);
    }

    private void publish(LockEventType type, LockStage stage, LockStatus status, Throwable error) {
        eventPublisher.publish(eventFactory.fromLease(lease, type, stage, status, error));
    }
}
