package com.xjtu.iron.distributed.lock.core.client;

import com.xjtu.iron.distributed.lock.api.status.LockStage;
import com.xjtu.iron.distributed.lock.api.status.LockStatus;
import com.xjtu.iron.distributed.lock.api.exception.LockLostException;
import com.xjtu.iron.distributed.lock.core.observability.LockEventFactory;
import com.xjtu.iron.distributed.lock.core.observability.LockEventPublisher;
import com.xjtu.iron.distributed.lock.core.observability.LockEventType;
import com.xjtu.iron.distributed.lock.core.observability.LockMetricsFacade;
import com.xjtu.iron.distributed.lock.core.execute.LockReleaseOutcome;
import com.xjtu.iron.distributed.lock.core.spi.LockAutoRenewMode;
import com.xjtu.iron.distributed.lock.core.spi.LockProvider;
import com.xjtu.iron.distributed.lock.core.spi.protocol.LockLease;
import com.xjtu.iron.distributed.lock.core.spi.protocol.LockCheckRequest;
import com.xjtu.iron.distributed.lock.core.spi.protocol.LockReleaseRequest;
import com.xjtu.iron.distributed.lock.core.spi.protocol.LockRenewRequest;
import com.xjtu.iron.distributed.lock.core.spi.protocol.LockCheckResponse;
import com.xjtu.iron.distributed.lock.core.spi.protocol.LockReleaseResponse;
import com.xjtu.iron.distributed.lock.core.spi.protocol.LockRenewResponse;
import com.xjtu.iron.distributed.lock.core.watchdog.LockWatchdog;
import com.xjtu.iron.distributed.lock.core.watchdog.WatchdogLockHandle;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
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

    /**
     * 与本 Handle 绑定的 watchdog。
     *
     * <p>watchdog 在 acquire 成功创建 Handle 时启动，而不是只在 execute(callback) 中启动。
     * 这样手工 tryLock(autoRenew=true) 与 execute(autoRenew=true) 具有一致语义。</p>
     */
    private final LockWatchdog watchdog;

    /**
     * 兼容原有直接构造场景；不绑定 watchdog。生产默认通过 LockHandleFactory 创建。
     */
    public DefaultLockHandle(LockProvider provider, LockLease lease, LockRuntimeState runtimeState, LockEventPublisher eventPublisher,
            LockEventFactory eventFactory, LockMetricsFacade metricsFacade) {
        this(provider, lease, runtimeState, eventPublisher, eventFactory, metricsFacade, null);
    }

    public DefaultLockHandle(LockProvider provider, LockLease lease, LockRuntimeState runtimeState, LockEventPublisher eventPublisher,
            LockEventFactory eventFactory, LockMetricsFacade metricsFacade, LockWatchdog watchdog) {
        this.provider = Objects.requireNonNull(provider, "provider must not be null");
        this.lease = Objects.requireNonNull(lease, "lease must not be null");
        this.runtimeState = Objects.requireNonNull(runtimeState, "runtimeState must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.eventFactory = Objects.requireNonNull(eventFactory, "eventFactory must not be null");
        this.metricsFacade = Objects.requireNonNull(metricsFacade, "metricsFacade must not be null");
        this.watchdog = watchdog;
    }

    public LockLease lease() { return lease; }
    public LockRuntimeState runtimeState() { return runtimeState; }
    @Override public String lockName() { return lease.getLockName(); }
    @Override public String lockKey() { return lease.getLockKey(); }
    @Override public String ownerToken() { return lease.getOwnerToken(); }
    @Override public OptionalLong fencingToken() { return lease.fencingToken(); }
    @Override public Optional<String> fencingTokenProviderName() { return lease.fencingTokenProviderName(); }
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
            stopWatchdog();
            return LockReleaseOutcome.alreadyAttempted();
        }
        try {
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
        } finally {
            // 手工 tryLock 也会启动 watchdog，因此释放时必须立即清理任务，不能等到下一个 tick。
            stopWatchdog();
        }
    }

    private void stopWatchdog() {
        if (watchdog != null) {
            watchdog.stop(this);
        }
    }

    @Override
    public void assertHeld() {
        if (!checkHeld()) { throw new LockLostException("lock lost: " + lease.getLockName()); }
    }

    @Override
    public LockAutoRenewMode autoRenewMode() {
        return provider.capabilities().getAutoRenewMode();
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
