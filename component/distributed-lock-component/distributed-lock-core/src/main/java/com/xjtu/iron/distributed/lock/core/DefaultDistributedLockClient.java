package com.xjtu.iron.distributed.lock.core;

import com.xjtu.iron.distributed.lock.api.DistributedLockClient;
import com.xjtu.iron.distributed.lock.api.LockCallback;
import com.xjtu.iron.distributed.lock.api.LockHandle;
import com.xjtu.iron.distributed.lock.api.LockOptions;
import com.xjtu.iron.distributed.lock.api.LockResult;
import com.xjtu.iron.distributed.lock.api.LockStage;
import com.xjtu.iron.distributed.lock.api.LockStatus;
import com.xjtu.iron.distributed.lock.api.exception.FencingTokenRejectedException;
import com.xjtu.iron.distributed.lock.api.exception.LockLostException;
import com.xjtu.iron.distributed.lock.api.exception.LockProviderException;
import com.xjtu.iron.distributed.lock.core.event.LockEvent;
import com.xjtu.iron.distributed.lock.core.event.LockEventPublisher;
import com.xjtu.iron.distributed.lock.core.event.LockEventType;
import com.xjtu.iron.distributed.lock.core.metrics.LockMetricsRecorder;
import com.xjtu.iron.distributed.lock.core.name.LockNamePatternResolver;
import com.xjtu.iron.distributed.lock.core.name.LockNameValidator;
import com.xjtu.iron.distributed.lock.core.runtime.LockRuntimeState;
import com.xjtu.iron.distributed.lock.core.spi.request.LockAcquireRequest;
import com.xjtu.iron.distributed.lock.core.spi.response.LockAcquireResponse;
import com.xjtu.iron.distributed.lock.core.spi.model.LockLease;
import com.xjtu.iron.distributed.lock.core.spi.LockProvider;
import com.xjtu.iron.distributed.lock.core.spi.LockProviderRegistry;
import com.xjtu.iron.distributed.lock.core.spi.response.LockReleaseResponse;
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
 */
public final class DefaultDistributedLockClient implements DistributedLockClient {

    private final LockProviderRegistry providerRegistry;
    private final OwnerTokenGenerator ownerTokenGenerator;
    private final LockWaiterFactory waiterFactory;
    private final LockWatchdog watchdog;
    private final LockEventPublisher eventPublisher;
    private final LockMetricsRecorder metricsRecorder;
    private final LockNameValidator lockNameValidator;
    private final LockNamePatternResolver patternResolver;
    private final Clock clock;

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
        this.providerRegistry = Objects.requireNonNull(providerRegistry, "providerRegistry must not be null");
        this.ownerTokenGenerator = Objects.requireNonNull(ownerTokenGenerator, "ownerTokenGenerator must not be null");
        this.waiterFactory = Objects.requireNonNull(waiterFactory, "waiterFactory must not be null");
        this.watchdog = Objects.requireNonNull(watchdog, "watchdog must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.metricsRecorder = Objects.requireNonNull(metricsRecorder, "metricsRecorder must not be null");
        this.lockNameValidator = Objects.requireNonNull(lockNameValidator, "lockNameValidator must not be null");
        this.patternResolver = Objects.requireNonNull(patternResolver, "patternResolver must not be null");
        this.clock = clock == null ? Clock.systemUTC() : clock;
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

            publishAttempt(provider, request);
            LockWaiter waiter = waiterFactory.getWaiter(actualOptions.getWaitStrategy());
            LockAcquireResponse response = waiter.waitForLock(new LockWaitContext(request, provider, clock));
            Duration waitDuration = Duration.between(start, Instant.now(clock));
            String pattern = patternResolver.resolvePattern(lockName);

            if (response.isAcquired()) {
                DefaultLockHandle handle = newHandle(provider, response.getLease());
                metricsRecorder.recordAcquire(provider.providerName(), actualOptions.getNamespace(), pattern, true, waitDuration);
                publish(handle.lease(), LockEventType.ACQUIRED, LockStage.ACQUIRE, LockStatus.ACQUIRED, null);
                return LockResult.acquired(handle, waitDuration);
            }

            if (response.hasError()) {
                Throwable error = response.getError() == null
                        ? new LockProviderException(response.getMessage())
                        : response.getError();
                metricsRecorder.recordAcquire(provider.providerName(), actualOptions.getNamespace(), pattern, false, waitDuration);
                publishProviderError(provider, request, LockStage.ACQUIRE, error);
                return LockResult.<LockHandle>builder()
                        .status(LockStatus.PROVIDER_ERROR)
                        .stage(LockStage.ACQUIRE)
                        .acquired(false)
                        .error(error)
                        .lockName(lockName)
                        .waitDuration(waitDuration)
                        .build();
            }

            metricsRecorder.recordAcquire(provider.providerName(), actualOptions.getNamespace(), pattern, false, waitDuration);
            LockStage notAcquiredStage = actualOptions.getWaitTime().isZero()
                    ? LockStage.ACQUIRE
                    : LockStage.WAIT;
            eventPublisher.publish(LockEvent.builder()
                    .eventType(LockEventType.NOT_ACQUIRED)
                    .stage(notAcquiredStage)
                    .status(LockStatus.NOT_ACQUIRED)
                    .namespace(actualOptions.getNamespace())
                    .lockName(lockName)
                    .providerName(provider.providerName())
                    .ownerToken(ownerToken)
                    .build());
            return LockResult.<LockHandle>builder()
                    .status(LockStatus.NOT_ACQUIRED)
                    .stage(notAcquiredStage)
                    .acquired(false)
                    .lockName(lockName)
                    .waitDuration(waitDuration)
                    .build();
        } catch (IllegalArgumentException e) {
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
        Instant start = Instant.now(clock);
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

        T value = null;
        LockStatus originalStatus = LockStatus.SUCCESS;
        LockStage originalStage = LockStage.EXECUTE;
        Throwable originalError = null;
        try {
            publish(handle.lease(), LockEventType.EXECUTION_STARTED, LockStage.EXECUTE, null, null);
            value = callback.doWithLock(handle);
            if (handle.isLost() && actualOptions.isFailOnLockLost()) {
                originalStatus = LockStatus.LOCK_LOST;
                originalStage = LockStage.RENEW;
                originalError = new LockLostException("lock lost during execution: " + lockName);
            }
        } catch (FencingTokenRejectedException e) {
            originalStatus = LockStatus.FENCING_REJECTED;
            originalStage = LockStage.FENCING;
            originalError = e;
            publish(handle.lease(), LockEventType.FENCING_REJECTED, originalStage, originalStatus, e);
        } catch (LockLostException e) {
            originalStatus = LockStatus.LOCK_LOST;
            originalStage = LockStage.CHECK;
            originalError = e;
            publish(handle.lease(), LockEventType.LOCK_LOST, originalStage, originalStatus, e);
        } catch (Throwable e) {
            originalStatus = LockStatus.EXECUTION_FAILED;
            originalStage = LockStage.EXECUTE;
            originalError = e;
            publish(handle.lease(), LockEventType.EXECUTION_FAILED, originalStage, originalStatus, e);
        }

        LockReleaseResponse releaseResponse;
        try {
            releaseResponse = handle.releaseWithResult();
        } finally {
            watchdog.stop(handle);
        }

        Duration holdDuration = Duration.between(handle.acquiredAt(), Instant.now(clock));
        Duration waitDuration = acquireResult.waitDuration();
        LockResult<T> result = mergeExecutionAndReleaseResult(
                value,
                handle,
                waitDuration,
                holdDuration,
                originalStatus,
                originalStage,
                originalError,
                releaseResponse,
                actualOptions
        );
        metricsRecorder.recordHold(handle.lease().getProviderName(), handle.lease().getNamespace(),
                patternResolver.resolvePattern(lockName), result.status(), holdDuration);
        if (result.status() == LockStatus.SUCCESS) {
            publish(handle.lease(), LockEventType.EXECUTION_SUCCESS, LockStage.EXECUTE, LockStatus.SUCCESS, null);
        }
        return result;
    }

    private LockOptions validate(String lockName, LockOptions options) {
        lockNameValidator.validate(lockName);
        LockOptions actualOptions = options == null ? LockOptions.defaults() : options;
        actualOptions.validate();
        return actualOptions;
    }

    private LockProvider selectProvider(LockOptions options) {
        return providerRegistry.getProvider(options.getProviderName());
    }

    private void validateProviderCapabilities(LockProvider provider, LockOptions options) {
        if (options.isAutoRenew() && !provider.capabilities().isAutoRenewSupported()) {
            throw new IllegalArgumentException("provider does not support auto renew: " + provider.providerName());
        }
        if (options.isFencingRequired() && !provider.capabilities().isFencingTokenSupported()) {
            throw new IllegalArgumentException("provider does not support fencing token: " + provider.providerName());
        }
    }

    private DefaultLockHandle newHandle(LockProvider provider, LockLease lease) {
        return new DefaultLockHandle(provider, lease, new LockRuntimeState(), eventPublisher,
                metricsRecorder, patternResolver);
    }

    private void publishAttempt(LockProvider provider, LockAcquireRequest request) {
        eventPublisher.publish(LockEvent.builder()
                .eventType(LockEventType.ACQUIRE_ATTEMPT)
                .stage(LockStage.ACQUIRE)
                .namespace(request.getNamespace())
                .lockName(request.getLockName())
                .providerName(provider.providerName())
                .ownerToken(request.getOwnerToken())
                .build());
    }

    private void publishProviderError(LockProvider provider, LockAcquireRequest request, LockStage stage, Throwable error) {
        eventPublisher.publish(LockEvent.builder()
                .eventType(LockEventType.PROVIDER_ERROR)
                .stage(stage)
                .status(LockStatus.PROVIDER_ERROR)
                .namespace(request.getNamespace())
                .lockName(request.getLockName())
                .providerName(provider.providerName())
                .ownerToken(request.getOwnerToken())
                .error(error)
                .build());
    }

    private void publish(LockLease lease, LockEventType eventType, LockStage stage, LockStatus status, Throwable error) {
        eventPublisher.publish(LockEvent.builder()
                .eventType(eventType)
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

    private <T> LockResult<T> mergeExecutionAndReleaseResult(
            T value,
            DefaultLockHandle handle,
            Duration waitDuration,
            Duration holdDuration,
            LockStatus originalStatus,
            LockStage originalStage,
            Throwable originalError,
            LockReleaseResponse releaseResponse,
            LockOptions options
    ) {
        LockStatus finalStatus = originalStatus;
        LockStage finalStage = originalStage;
        Throwable finalError = originalError;

        if (releaseResponse.getStatus() == null) {
            finalStatus = LockStatus.RELEASE_FAILED;
            finalStage = LockStage.RELEASE;
        } else {
            switch (releaseResponse.getStatus()) {
                case RELEASED:
                    break;
                case NOT_FOUND:
                case NOT_OWNER:
                    if (originalStatus == LockStatus.SUCCESS && options.isFailOnLockLost()) {
                        finalStatus = LockStatus.LOCK_LOST;
                        finalStage = LockStage.RELEASE;
                        finalError = new LockLostException("lock lost on release: " + handle.lockName());
                    }
                    break;
                case PROVIDER_ERROR:
                default:
                    if (originalStatus == LockStatus.SUCCESS) {
                        finalStatus = LockStatus.RELEASE_FAILED;
                        finalStage = LockStage.RELEASE;
                        finalError = releaseResponse.getError();
                    } else if (originalError != null && releaseResponse.getError() != null) {
                        originalError.addSuppressed(releaseResponse.getError());
                    }
                    break;
            }
        }

        return LockResult.<T>builder()
                .status(finalStatus)
                .stage(finalStage)
                .acquired(true)
                .value(finalStatus == LockStatus.SUCCESS ? value : null)
                .handle(handle)
                .error(finalError)
                .lockName(handle.lockName())
                .lockKey(handle.lockKey())
                .ownerToken(handle.ownerToken())
                .fencingToken(handle.fencingToken().isPresent() ? handle.fencingToken().getAsLong() : null)
                .waitDuration(waitDuration)
                .holdDuration(holdDuration)
                .build();
    }
}
