package com.xjtu.iron.distributed.lock.core.execute;

import com.xjtu.iron.distributed.lock.api.LockCallback;
import com.xjtu.iron.distributed.lock.api.LockHandle;
import com.xjtu.iron.distributed.lock.api.LockOptions;
import com.xjtu.iron.distributed.lock.api.LockResult;
import com.xjtu.iron.distributed.lock.api.LockStage;
import com.xjtu.iron.distributed.lock.api.LockStatus;
import com.xjtu.iron.distributed.lock.api.exception.FencingTokenRejectedException;
import com.xjtu.iron.distributed.lock.api.exception.LockLostException;
import com.xjtu.iron.distributed.lock.core.DefaultLockHandle;
import com.xjtu.iron.distributed.lock.core.acquire.LockAcquisitionService;
import com.xjtu.iron.distributed.lock.core.event.LockEventFactory;
import com.xjtu.iron.distributed.lock.core.event.LockEventPublisher;
import com.xjtu.iron.distributed.lock.core.event.LockEventType;
import com.xjtu.iron.distributed.lock.core.metrics.LockMetricsFacade;
import com.xjtu.iron.distributed.lock.core.result.ExecutionOutcome;
import com.xjtu.iron.distributed.lock.core.result.LockReleaseOutcome;
import com.xjtu.iron.distributed.lock.core.result.LockResultResolver;
import com.xjtu.iron.distributed.lock.core.watchdog.LockWatchdog;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 持锁业务执行模板。
 *
 * <p>这里保留 callback 异常分类、watchdog、release 和最终结果归并，因为它们共同组成一次
 * execute 生命周期。没有再为每一个 private 方法额外创建 Invoker、Coordinator、Mapper，
 * 避免把一条连续流程拆成过多跳转。</p>
 */
public final class LockExecutionTemplate {

    private final LockAcquisitionService acquisitionService;
    private final LockWatchdog watchdog;
    private final LockEventPublisher eventPublisher;
    private final LockEventFactory eventFactory;
    private final LockMetricsFacade metricsFacade;
    private final Clock clock;
    private final LockResultResolver resultResolver;

    public LockExecutionTemplate(
            LockAcquisitionService acquisitionService,
            LockWatchdog watchdog,
            LockEventPublisher eventPublisher,
            LockEventFactory eventFactory,
            LockMetricsFacade metricsFacade,
            Clock clock,
            LockResultResolver resultResolver
    ) {
        this.acquisitionService = Objects.requireNonNull(
                acquisitionService, "acquisitionService must not be null");
        this.watchdog = Objects.requireNonNull(watchdog, "watchdog must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.eventFactory = Objects.requireNonNull(eventFactory, "eventFactory must not be null");
        this.metricsFacade = Objects.requireNonNull(metricsFacade, "metricsFacade must not be null");
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.resultResolver = Objects.requireNonNull(resultResolver, "resultResolver must not be null");
    }

    public <T> LockResult<T> execute(String lockName, LockOptions options, LockCallback<T> callback) {
        Objects.requireNonNull(callback, "callback must not be null");

        LockAcquisitionService.AcquireAttempt attempt = acquisitionService.acquire(lockName, options);
        LockResult<LockHandle> acquireResult = attempt.result();
        if (acquireResult.status() != LockStatus.ACQUIRED) {
            return copyAcquireFailure(acquireResult);
        }

        DefaultLockHandle handle = requireDefaultHandle(acquireResult);
        LockOptions actualOptions = attempt.requireOptions();
        if (actualOptions.isAutoRenew()) {
            watchdog.start(handle, actualOptions);
        }

        ExecutionOutcome<T> executionOutcome = invokeCallback(
                lockName,
                handle,
                actualOptions,
                callback);

        LockReleaseOutcome releaseOutcome;
        try {
            releaseOutcome = handle.releaseWithOutcome();
        } finally {
            watchdog.stop(handle);
        }

        Duration holdDuration = Duration.between(handle.acquiredAt(), Instant.now(clock));
        LockResult<T> result = resultResolver.resolve(
                executionOutcome,
                releaseOutcome,
                handle,
                acquireResult.waitDuration(),
                holdDuration,
                actualOptions);

        metricsFacade.recordHold(handle.lease(), result.status(), holdDuration);
        if (result.status() == LockStatus.SUCCESS) {
            eventPublisher.publish(eventFactory.fromLease(
                    handle.lease(),
                    LockEventType.EXECUTION_SUCCESS,
                    LockStage.EXECUTE,
                    LockStatus.SUCCESS,
                    null));
        }
        return result;
    }

    private <T> ExecutionOutcome<T> invokeCallback(String lockName, DefaultLockHandle handle, LockOptions options, LockCallback<T> callback) {
        try {
            eventPublisher.publish(eventFactory.fromLease(
                    handle.lease(),
                    LockEventType.EXECUTION_STARTED,
                    LockStage.EXECUTE,
                    null,
                    null));

            T value = callback.doWithLock(handle);
            if (handle.isLost() && options.isFailOnLockLost()) {
                return ExecutionOutcome.failure(
                        LockStatus.LOCK_LOST,
                        LockStage.RENEW,
                        new LockLostException("lock lost during execution: " + lockName));
            }
            return ExecutionOutcome.success(value);
        } catch (FencingTokenRejectedException error) {
            eventPublisher.publish(eventFactory.fromLease(
                    handle.lease(),
                    LockEventType.FENCING_REJECTED,
                    LockStage.FENCING,
                    LockStatus.FENCING_REJECTED,
                    error));
            return ExecutionOutcome.failure(
                    LockStatus.FENCING_REJECTED,
                    LockStage.FENCING,
                    error);
        } catch (LockLostException error) {
            eventPublisher.publish(eventFactory.fromLease(
                    handle.lease(),
                    LockEventType.LOCK_LOST,
                    LockStage.CHECK,
                    LockStatus.LOCK_LOST,
                    error));
            return ExecutionOutcome.failure(
                    LockStatus.LOCK_LOST,
                    LockStage.CHECK,
                    error);
        } catch (Throwable error) {
            eventPublisher.publish(eventFactory.fromLease(
                    handle.lease(),
                    LockEventType.EXECUTION_FAILED,
                    LockStage.EXECUTE,
                    LockStatus.EXECUTION_FAILED,
                    error));
            return ExecutionOutcome.failure(
                    LockStatus.EXECUTION_FAILED,
                    LockStage.EXECUTE,
                    error);
        }
    }

    private DefaultLockHandle requireDefaultHandle(LockResult<LockHandle> acquireResult) {
        LockHandle handle = acquireResult.handle()
                .orElseThrow(() -> new IllegalStateException("acquired result has no handle"));
        if (!(handle instanceof DefaultLockHandle)) {
            throw new IllegalStateException(
                    "acquired handle is not managed by DefaultDistributedLockClient: "
                            + handle.getClass().getName());
        }
        return (DefaultLockHandle) handle;
    }

    private <T> LockResult<T> copyAcquireFailure(LockResult<LockHandle> acquireResult) {
        return LockResult.<T>builder()
                .status(acquireResult.status())
                .stage(acquireResult.stage())
                .acquired(acquireResult.acquired())
                .error(acquireResult.error().orElse(null))
                .lockName(acquireResult.lockName())
                .lockKey(acquireResult.lockKey())
                .ownerToken(acquireResult.ownerToken())
                .fencingToken(acquireResult.fencingToken().orElse(null))
                .fencingTokenProviderName(acquireResult.fencingTokenProviderName().orElse(null))
                .waitDuration(acquireResult.waitDuration())
                .build();
    }
}
