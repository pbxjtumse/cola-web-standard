package com.xjtu.iron.distributed.lock.core;

import com.xjtu.iron.distributed.lock.api.DistributedLockClient;
import com.xjtu.iron.distributed.lock.api.LockCallback;
import com.xjtu.iron.distributed.lock.api.LockHandle;
import com.xjtu.iron.distributed.lock.api.LockOptions;
import com.xjtu.iron.distributed.lock.api.LockResult;
import com.xjtu.iron.distributed.lock.api.LockStage;
import com.xjtu.iron.distributed.lock.api.LockStatus;
import com.xjtu.iron.distributed.lock.api.exception.FencingTokenRejectedException;
import com.xjtu.iron.distributed.lock.api.exception.InvalidLockOptionsException;
import com.xjtu.iron.distributed.lock.api.exception.LockLostException;
import com.xjtu.iron.distributed.lock.api.exception.LockProviderException;
import com.xjtu.iron.distributed.lock.core.event.LockEventFactory;
import com.xjtu.iron.distributed.lock.core.event.LockEventPublisher;
import com.xjtu.iron.distributed.lock.core.event.LockEventType;
import com.xjtu.iron.distributed.lock.core.metrics.LockMetricsFacade;
import com.xjtu.iron.distributed.lock.core.metrics.LockMetricsRecorder;
import com.xjtu.iron.distributed.lock.core.name.LockNamePatternResolver;
import com.xjtu.iron.distributed.lock.core.name.LockNameValidator;
import com.xjtu.iron.distributed.lock.core.result.ExecutionOutcome;
import com.xjtu.iron.distributed.lock.core.result.LockReleaseOutcome;
import com.xjtu.iron.distributed.lock.core.result.LockResultResolver;
import com.xjtu.iron.distributed.lock.core.runtime.LockRuntimeState;
import com.xjtu.iron.distributed.lock.core.spi.LockProvider;
import com.xjtu.iron.distributed.lock.core.spi.LockProviderRegistry;
import com.xjtu.iron.distributed.lock.core.spi.model.LockLease;
import com.xjtu.iron.distributed.lock.core.spi.request.LockAcquireRequest;
import com.xjtu.iron.distributed.lock.core.spi.response.LockAcquireResponse;
import com.xjtu.iron.distributed.lock.core.token.OwnerTokenGenerator;
import com.xjtu.iron.distributed.lock.core.wait.LockWaitContext;
import com.xjtu.iron.distributed.lock.core.wait.LockWaiter;
import com.xjtu.iron.distributed.lock.core.wait.LockWaiterFactory;
import com.xjtu.iron.distributed.lock.core.watchdog.LockWatchdog;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 默认分布式锁客户端实现。
 *
 * <p>本类负责把 API 层调用编排为完整生命周期：参数校验、生成 ownerToken、选择 Provider、等待加锁、创建
 * LockHandle、启动 watchdog、执行业务 callback、finally 释放锁、停止 watchdog、记录事件和指标。</p>
 *
 * <p>底层 Redis/ZK/Etcd 的原子操作不在本类实现，而是委托给 {@link LockProvider}。</p>
 * 1. waitTime = 0，没有真正等待，第一次抢锁失败：
 *     status=NOT_ACQUIRED
 *     stage=ACQUIRE
 *
 *    waitTime > 0，等待重试后超时：
 *     status=NOT_ACQUIRED
 *     stage=WAIT
 */
public final class DefaultDistributedLockClient implements DistributedLockClient {

    private final LockProviderRegistry providerRegistry;
    private final OwnerTokenGenerator ownerTokenGenerator;
    private final LockWaiterFactory waiterFactory;
    private final LockWatchdog watchdog;
    private final LockEventPublisher eventPublisher;
    private final LockEventFactory eventFactory;
    private final LockMetricsFacade metricsFacade;
    private final LockNameValidator lockNameValidator;
    private final Clock clock;
    private final LockResultResolver resultResolver;

    public DefaultDistributedLockClient(
            LockProviderRegistry providerRegistry,
            OwnerTokenGenerator ownerTokenGenerator,
            LockWaiterFactory waiterFactory,
            LockWatchdog watchdog,
            LockEventPublisher eventPublisher,
            LockMetricsRecorder metricsRecorder,
            LockNameValidator lockNameValidator,
            LockNamePatternResolver patternResolver,
            Clock clock
    ) {
        this(providerRegistry, ownerTokenGenerator, waiterFactory, watchdog, eventPublisher,
                new LockEventFactory(), new LockMetricsFacade(metricsRecorder, patternResolver),
                lockNameValidator, clock, new LockResultResolver());
    }

    public DefaultDistributedLockClient(
            LockProviderRegistry providerRegistry,
            OwnerTokenGenerator ownerTokenGenerator,
            LockWaiterFactory waiterFactory,
            LockWatchdog watchdog,
            LockEventPublisher eventPublisher,
            LockEventFactory eventFactory,
            LockMetricsFacade metricsFacade,
            LockNameValidator lockNameValidator,
            Clock clock,
            LockResultResolver resultResolver
    ) {
        this.providerRegistry = Objects.requireNonNull(providerRegistry, "providerRegistry must not be null");
        this.ownerTokenGenerator = Objects.requireNonNull(ownerTokenGenerator, "ownerTokenGenerator must not be null");
        this.waiterFactory = Objects.requireNonNull(waiterFactory, "waiterFactory must not be null");
        this.watchdog = Objects.requireNonNull(watchdog, "watchdog must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.eventFactory = Objects.requireNonNull(eventFactory, "eventFactory must not be null");
        this.metricsFacade = Objects.requireNonNull(metricsFacade, "metricsFacade must not be null");
        this.lockNameValidator = Objects.requireNonNull(lockNameValidator, "lockNameValidator must not be null");
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.resultResolver = Objects.requireNonNull(resultResolver, "resultResolver must not be null");
    }

    @Override
    public LockResult<LockHandle> tryLock(String lockName, LockOptions options) {
        Instant start = Instant.now(clock);
        try {
            LockOptions actualOptions = validate(lockName, options);
            LockProvider provider = selectProvider(actualOptions);
            validateProviderCapabilities(provider, actualOptions);
            String ownerToken = ownerTokenGenerator.generate(actualOptions.getNamespace(), lockName);
            LockAcquireRequest request = LockAcquireRequest.builder()
                    .lockName(lockName)
                    .ownerToken(ownerToken)
                    .options(actualOptions)
                    .build();
            eventPublisher.publish(eventFactory.fromAcquireRequest(provider, request, LockEventType.ACQUIRE_ATTEMPT, LockStage.ACQUIRE, null, null));
            LockWaiter waiter = waiterFactory.getWaiter(actualOptions.getWaitStrategy());
            LockAcquireResponse response = waiter.waitForLock(new LockWaitContext(request, provider, clock));
            Duration waitDuration = Duration.between(start, Instant.now(clock));

            if (response.isAcquired()) {
                DefaultLockHandle handle = newHandle(provider, response.getLease());
                metricsFacade.recordAcquire(provider.providerName(), actualOptions.getNamespace(), lockName, true, waitDuration);
                eventPublisher.publish(eventFactory.fromLease(handle.lease(), LockEventType.ACQUIRED, LockStage.ACQUIRE, LockStatus.ACQUIRED, null));
                return LockResult.acquired(handle, waitDuration);
            }

            if (response.hasProviderError()) {
                Throwable error = response.getError() == null ? new LockProviderException(response.getMessage()) : response.getError();
                metricsFacade.recordAcquire(provider.providerName(), actualOptions.getNamespace(), lockName, false, waitDuration);
                eventPublisher.publish(eventFactory.fromAcquireRequest(provider, request, LockEventType.PROVIDER_ERROR, LockStage.ACQUIRE, LockStatus.PROVIDER_ERROR, error));
                return LockResult.<LockHandle>builder()
                        .status(LockStatus.PROVIDER_ERROR)
                        .stage(LockStage.ACQUIRE)
                        .acquired(false)
                        .error(error)
                        .lockName(lockName)
                        .waitDuration(waitDuration)
                        .build();
            }

            metricsFacade.recordAcquire(provider.providerName(), actualOptions.getNamespace(), lockName, false, waitDuration);
            LockStage notAcquiredStage = actualOptions.getWaitTime().isZero() ? LockStage.ACQUIRE : LockStage.WAIT;
            eventPublisher.publish(eventFactory.fromAcquireRequest(provider, request, LockEventType.NOT_ACQUIRED, notAcquiredStage, LockStatus.NOT_ACQUIRED, null));
            return LockResult.notAcquired(lockName, null, notAcquiredStage, waitDuration);
        } catch (IllegalArgumentException | InvalidLockOptionsException e) {
            return LockResult.<LockHandle>builder()
                    .status(LockStatus.INVALID_OPTIONS)
                    .stage(LockStage.VALIDATE)
                    .acquired(false)
                    .error(e)
                    .lockName(lockName)
                    .waitDuration(Duration.between(start, Instant.now(clock)))
                    .build();
        }
    }

    @Override
    public <T> LockResult<T> execute(String lockName, LockOptions options, LockCallback<T> callback) {
        Objects.requireNonNull(callback, "callback must not be null");
        LockResult<LockHandle> acquireResult = tryLock(lockName, options);
        if (!acquireResult.isAcquired()) {
            return copyAcquireFailure(acquireResult);
        }
        DefaultLockHandle handle = (DefaultLockHandle) acquireResult.handle()
                .orElseThrow(() -> new IllegalStateException("acquired result has no handle"));
        LockOptions actualOptions = options == null ? LockOptions.defaults() : options;
        if (actualOptions.isAutoRenew()) {
            watchdog.start(handle, actualOptions);
        }

        ExecutionOutcome<T> executionOutcome;
        try {
            eventPublisher.publish(eventFactory.fromLease(handle.lease(), LockEventType.EXECUTION_STARTED, LockStage.EXECUTE, null, null));
            T value = callback.doWithLock(handle);
            if (handle.isLost() && actualOptions.isFailOnLockLost()) {
                executionOutcome = ExecutionOutcome.failure(LockStatus.LOCK_LOST, LockStage.RENEW,
                        new LockLostException("lock lost during execution: " + lockName));
            } else {
                executionOutcome = ExecutionOutcome.success(value);
            }
        } catch (FencingTokenRejectedException e) {
            executionOutcome = ExecutionOutcome.failure(LockStatus.FENCING_REJECTED, LockStage.FENCING, e);
            eventPublisher.publish(eventFactory.fromLease(handle.lease(), LockEventType.FENCING_REJECTED, LockStage.FENCING, LockStatus.FENCING_REJECTED, e));
        } catch (LockLostException e) {
            executionOutcome = ExecutionOutcome.failure(LockStatus.LOCK_LOST, LockStage.CHECK, e);
            eventPublisher.publish(eventFactory.fromLease(handle.lease(), LockEventType.LOCK_LOST, LockStage.CHECK, LockStatus.LOCK_LOST, e));
        } catch (Throwable e) {
            executionOutcome = ExecutionOutcome.failure(LockStatus.EXECUTION_FAILED, LockStage.EXECUTE, e);
            eventPublisher.publish(eventFactory.fromLease(handle.lease(), LockEventType.EXECUTION_FAILED, LockStage.EXECUTE, LockStatus.EXECUTION_FAILED, e));
        }

        LockReleaseOutcome releaseOutcome;
        try {
            releaseOutcome = handle.releaseWithOutcome();
        } finally {
            watchdog.stop(handle);
        }

        Duration holdDuration = Duration.between(handle.acquiredAt(), Instant.now(clock));
        LockResult<T> result = resultResolver.resolve(executionOutcome, releaseOutcome, handle,
                acquireResult.waitDuration(), holdDuration, actualOptions);
        metricsFacade.recordHold(handle.lease(), result.status(), holdDuration);
        if (result.status() == LockStatus.SUCCESS) {
            eventPublisher.publish(eventFactory.fromLease(handle.lease(), LockEventType.EXECUTION_SUCCESS, LockStage.EXECUTE, LockStatus.SUCCESS, null));
        }
        return result;
    }

    private LockOptions validate(String lockName, LockOptions options) {
        lockNameValidator.validate(lockName);
        LockOptions actualOptions = options == null ? LockOptions.defaults() : options;
        actualOptions.validate();
        return actualOptions;
    }

    private LockProvider selectProvider(LockOptions options) { return providerRegistry.getProvider(options.getProviderName()); }

    private void validateProviderCapabilities(LockProvider provider, LockOptions options) {
        if (options.isAutoRenew() && !provider.capabilities().isAutoRenewSupported()) {
            throw new IllegalArgumentException("provider does not support auto renew: " + provider.providerName());
        }
        if (options.isFencingRequired() && !provider.capabilities().isFencingTokenSupported()) {
            throw new IllegalArgumentException("provider does not support fencing token: " + provider.providerName());
        }
    }

    private DefaultLockHandle newHandle(LockProvider provider, LockLease lease) {
        return new DefaultLockHandle(provider, lease, new LockRuntimeState(), eventPublisher, eventFactory, metricsFacade);
    }

    private <T> LockResult<T> copyAcquireFailure(LockResult<LockHandle> acquireResult) {
        return LockResult.<T>builder()
                .status(acquireResult.status())
                .stage(acquireResult.stage())
                .acquired(false)
                .error(acquireResult.error().orElse(null))
                .lockName(acquireResult.lockName())
                .lockKey(acquireResult.lockKey())
                .ownerToken(acquireResult.ownerToken())
                .waitDuration(acquireResult.waitDuration())
                .build();
    }
}
