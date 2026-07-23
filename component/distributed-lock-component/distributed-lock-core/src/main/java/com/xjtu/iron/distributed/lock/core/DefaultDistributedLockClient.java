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
import com.xjtu.iron.distributed.lock.core.fencing.DefaultFencingTokenProviderRegistry;
import com.xjtu.iron.distributed.lock.core.fencing.FencingTokenCoordinator;
import com.xjtu.iron.distributed.lock.core.fencing.FencingTokenPlan;
import com.xjtu.iron.distributed.lock.core.fencing.FencingTokenResponse;
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
import com.xjtu.iron.distributed.lock.core.spi.request.LockCheckRequest;
import com.xjtu.iron.distributed.lock.core.spi.request.LockReleaseRequest;
import com.xjtu.iron.distributed.lock.core.spi.response.LockAcquireResponse;
import com.xjtu.iron.distributed.lock.core.spi.response.LockCheckResponse;
import com.xjtu.iron.distributed.lock.core.spi.response.LockReleaseResponse;
import com.xjtu.iron.distributed.lock.core.token.OwnerTokenGenerator;
import com.xjtu.iron.distributed.lock.core.wait.LockWaitContext;
import com.xjtu.iron.distributed.lock.core.wait.LockWaiter;
import com.xjtu.iron.distributed.lock.core.wait.LockWaiterFactory;
import com.xjtu.iron.distributed.lock.core.watchdog.LockWatchdog;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;

/**
 * 默认分布式锁客户端实现。
 *
 * <p>一期负责锁生命周期编排；二期在不破坏原有 API 的前提下增加 fencing token 计划选择：</p>
 * <ul>
 *     <li>Redis 等锁 Provider 原生发号：在原子 acquire 中生成 token；</li>
 *     <li>独立 Provider 发号：先获取互斥锁，再通过 JDBC sequence 等 Provider 生成 token；</li>
 *     <li>独立发号失败：立即释放已经获取的锁，并返回 PROVIDER_ERROR + FENCING。</li>
 * </ul>
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
    private final LockOptions defaultOptions;
    private final Clock clock;
    private final LockResultResolver resultResolver;
    private final FencingTokenCoordinator fencingTokenCoordinator;

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
        this(providerRegistry, ownerTokenGenerator, waiterFactory, watchdog, eventPublisher, metricsRecorder,
                lockNameValidator, patternResolver, LockOptions.defaults(), clock);
    }

    public DefaultDistributedLockClient(
            LockProviderRegistry providerRegistry,
            OwnerTokenGenerator ownerTokenGenerator,
            LockWaiterFactory waiterFactory,
            LockWatchdog watchdog,
            LockEventPublisher eventPublisher,
            LockMetricsRecorder metricsRecorder,
            LockNameValidator lockNameValidator,
            LockNamePatternResolver patternResolver,
            LockOptions defaultOptions,
            Clock clock
    ) {
        this(providerRegistry, ownerTokenGenerator, waiterFactory, watchdog, eventPublisher,
                new LockEventFactory(), new LockMetricsFacade(metricsRecorder, patternResolver),
                lockNameValidator, defaultOptions, clock, new LockResultResolver(),
                new FencingTokenCoordinator(new DefaultFencingTokenProviderRegistry(Collections.emptyList())));
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
        this(providerRegistry, ownerTokenGenerator, waiterFactory, watchdog, eventPublisher,
                eventFactory, metricsFacade, lockNameValidator, LockOptions.defaults(), clock, resultResolver,
                new FencingTokenCoordinator(new DefaultFencingTokenProviderRegistry(Collections.emptyList())));
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
            LockOptions defaultOptions,
            Clock clock,
            LockResultResolver resultResolver
    ) {
        this(providerRegistry, ownerTokenGenerator, waiterFactory, watchdog, eventPublisher,
                eventFactory, metricsFacade, lockNameValidator, defaultOptions, clock, resultResolver,
                new FencingTokenCoordinator(new DefaultFencingTokenProviderRegistry(Collections.emptyList())));
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
            LockOptions defaultOptions,
            Clock clock,
            LockResultResolver resultResolver,
            FencingTokenCoordinator fencingTokenCoordinator
    ) {
        this.providerRegistry = Objects.requireNonNull(providerRegistry, "providerRegistry must not be null");
        this.ownerTokenGenerator = Objects.requireNonNull(ownerTokenGenerator, "ownerTokenGenerator must not be null");
        this.waiterFactory = Objects.requireNonNull(waiterFactory, "waiterFactory must not be null");
        this.watchdog = Objects.requireNonNull(watchdog, "watchdog must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.eventFactory = Objects.requireNonNull(eventFactory, "eventFactory must not be null");
        this.metricsFacade = Objects.requireNonNull(metricsFacade, "metricsFacade must not be null");
        this.lockNameValidator = Objects.requireNonNull(lockNameValidator, "lockNameValidator must not be null");
        this.defaultOptions = defaultOptions == null ? LockOptions.defaults() : defaultOptions;
        this.defaultOptions.validate();
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.resultResolver = Objects.requireNonNull(resultResolver, "resultResolver must not be null");
        this.fencingTokenCoordinator = Objects.requireNonNull(
                fencingTokenCoordinator, "fencingTokenCoordinator must not be null");
    }

    @Override
    public LockResult<LockHandle> tryLock(String lockName, LockOptions options) {
        Instant start = Instant.now(clock);
        try {
            LockOptions actualOptions = validate(lockName, options);
            LockProvider provider = selectProvider(actualOptions);
            FencingTokenPlan fencingPlan = fencingTokenCoordinator.plan(provider, actualOptions);
            validateProviderCapabilities(provider, actualOptions, fencingPlan);

            String ownerToken = ownerTokenGenerator.generate(actualOptions.getNamespace(), lockName);
            LockAcquireRequest request = LockAcquireRequest.builder()
                    .lockName(lockName)
                    .ownerToken(ownerToken)
                    .options(actualOptions)
                    .nativeFencingRequired(fencingPlan.isNative())
                    .build();

            eventPublisher.publish(eventFactory.fromAcquireRequest(provider, request,
                    LockEventType.ACQUIRE_ATTEMPT, LockStage.ACQUIRE, null, null));
            LockWaiter waiter = waiterFactory.getWaiter(actualOptions.getWaitStrategy());
            LockAcquireResponse response = waiter.waitForLock(new LockWaitContext(request, provider, clock));
            Duration waitDuration = Duration.between(start, Instant.now(clock));

            if (response.isAcquired()) {
                LockLease lease = response.getLease();
                metricsFacade.recordAcquire(provider.providerName(), actualOptions.getNamespace(),
                        lockName, true, waitDuration);
                eventPublisher.publish(eventFactory.fromLease(lease, LockEventType.ACQUIRED,
                        LockStage.ACQUIRE, LockStatus.ACQUIRED, null));

                LockResult<LockHandle> nativeFencingFailure = completeFencing(
                        provider, actualOptions, fencingPlan, lease, waitDuration);
                if (nativeFencingFailure != null) {
                    return nativeFencingFailure;
                }

                if (fencingPlan.isExternal()) {
                    String source = fencingPlan.sourceName(provider.providerName());
                    Instant fencingStart = Instant.now(clock);
                    FencingTokenResponse tokenResponse = fencingTokenCoordinator.issueExternal(
                            fencingPlan, lease, actualOptions);
                    Duration fencingDuration = Duration.between(fencingStart, Instant.now(clock));
                    if (!tokenResponse.isIssued()) {
                        return fencingFailure(provider, lease, waitDuration, source,
                                tokenResponse, fencingDuration);
                    }
                    long token = tokenResponse.token().orElseThrow();
                    lease = lease.withFencingToken(token, source);
                    metricsFacade.recordFencing(provider.providerName(), source,
                            lease.getNamespace(), true, fencingDuration);
                    eventPublisher.publish(eventFactory.fromLease(lease,
                            LockEventType.FENCING_TOKEN_ISSUED, LockStage.FENCING, null, null));

                    /*
                     * 外部发号可能比 leaseTime 更慢。发号完成后必须重新确认 ownerToken 仍然持锁，
                     * 避免把“已经过期的租约 + 后生成的较大 token”交给业务 callback。
                     */
                    LockResult<LockHandle> ownershipFailure = verifyOwnershipAfterExternalFencing(
                            provider, lease, waitDuration);
                    if (ownershipFailure != null) {
                        return ownershipFailure;
                    }
                }

                DefaultLockHandle handle = newHandle(provider, lease);
                return LockResult.acquired(handle, waitDuration);
            }

            if (response.hasProviderError()) {
                Throwable error = response.getError() == null
                        ? new LockProviderException(response.getMessage())
                        : response.getError();
                metricsFacade.recordAcquire(provider.providerName(), actualOptions.getNamespace(),
                        lockName, false, waitDuration);
                eventPublisher.publish(eventFactory.fromAcquireRequest(provider, request,
                        LockEventType.PROVIDER_ERROR, LockStage.ACQUIRE, LockStatus.PROVIDER_ERROR, error));
                return LockResult.<LockHandle>builder()
                        .status(LockStatus.PROVIDER_ERROR)
                        .stage(LockStage.ACQUIRE)
                        .acquired(false)
                        .error(error)
                        .lockName(lockName)
                        .waitDuration(waitDuration)
                        .build();
            }

            metricsFacade.recordAcquire(provider.providerName(), actualOptions.getNamespace(),
                    lockName, false, waitDuration);
            LockStage notAcquiredStage = actualOptions.getWaitTime().isZero()
                    ? LockStage.ACQUIRE : LockStage.WAIT;
            eventPublisher.publish(eventFactory.fromAcquireRequest(provider, request,
                    LockEventType.NOT_ACQUIRED, notAcquiredStage, LockStatus.NOT_ACQUIRED, null));
            return LockResult.notAcquired(lockName, null, notAcquiredStage, waitDuration);
        } catch (IllegalArgumentException | InvalidLockOptionsException error) {
            return LockResult.<LockHandle>builder()
                    .status(LockStatus.INVALID_OPTIONS)
                    .stage(LockStage.VALIDATE)
                    .acquired(false)
                    .error(error)
                    .lockName(lockName)
                    .waitDuration(Duration.between(start, Instant.now(clock)))
                    .build();
        }
    }

    /**
     * 校验原生 fencing 返回结果。外部 fencing 在后续单独发号。
     * 返回 null 表示可以继续。
     */
    private LockResult<LockHandle> completeFencing(
            LockProvider provider,
            LockOptions options,
            FencingTokenPlan plan,
            LockLease lease,
            Duration waitDuration
    ) {
        if (!plan.isNative()) {
            return null;
        }
        if (lease.fencingToken().isEmpty()) {
            FencingTokenResponse response = FencingTokenResponse.failed(
                    new LockProviderException("native fencing token is missing: " + provider.providerName()));
            return fencingFailure(provider, lease, waitDuration, provider.providerName(), response, Duration.ZERO);
        }
        metricsFacade.recordFencing(provider.providerName(), provider.providerName(),
                options.getNamespace(), true, Duration.ZERO);
        eventPublisher.publish(eventFactory.fromLease(lease,
                LockEventType.FENCING_TOKEN_ISSUED, LockStage.FENCING, null, null));
        return null;
    }

    private LockResult<LockHandle> fencingFailure(
            LockProvider lockProvider,
            LockLease lease,
            Duration waitDuration,
            String fencingProviderName,
            FencingTokenResponse tokenResponse,
            Duration fencingDuration
    ) {
        Throwable error = tokenResponse.getError();
        if (error == null) {
            error = new LockProviderException(tokenResponse.getMessage() == null
                    ? "failed to issue fencing token"
                    : tokenResponse.getMessage());
        }

        releaseAfterPreparationFailure(lockProvider, lease, error);

        metricsFacade.recordFencing(lockProvider.providerName(), fencingProviderName,
                lease.getNamespace(), false, fencingDuration == null ? Duration.ZERO : fencingDuration);
        eventPublisher.publish(eventFactory.fromFencing(lease,
                LockEventType.FENCING_TOKEN_FAILED, LockStatus.PROVIDER_ERROR,
                fencingProviderName, error));

        return LockResult.<LockHandle>builder()
                .status(LockStatus.PROVIDER_ERROR)
                .stage(LockStage.FENCING)
                .acquired(true)
                .error(error)
                .lockName(lease.getLockName())
                .lockKey(lease.getLockKey())
                .ownerToken(lease.getOwnerToken())
                .fencingTokenProviderName(fencingProviderName)
                .waitDuration(waitDuration)
                .build();
    }

    /**
     * 外部 fencing token 发号完成后重新确认锁归属。
     *
     * @return null 表示仍然持锁；否则返回不允许进入 callback 的最终结果。
     */
    private LockResult<LockHandle> verifyOwnershipAfterExternalFencing(
            LockProvider lockProvider,
            LockLease lease,
            Duration waitDuration
    ) {
        LockCheckResponse response;
        try {
            response = lockProvider.check(LockCheckRequest.fromLease(lease));
        } catch (Throwable error) {
            releaseAfterPreparationFailure(lockProvider, lease, error);
            eventPublisher.publish(eventFactory.fromLease(lease,
                    LockEventType.PROVIDER_ERROR, LockStage.CHECK,
                    LockStatus.PROVIDER_ERROR, error));
            return preparationCheckFailure(lease, waitDuration, error);
        }

        if (response.isHeld()) {
            return null;
        }
        if (response.isLockLost()) {
            metricsFacade.recordLost(lease);
            eventPublisher.publish(eventFactory.fromLease(lease,
                    LockEventType.LOCK_LOST, LockStage.CHECK, LockStatus.LOCK_LOST, null));
            return LockResult.<LockHandle>builder()
                    .status(LockStatus.LOCK_LOST)
                    .stage(LockStage.CHECK)
                    .acquired(true)
                    .error(new LockLostException(
                            "lock lost while issuing external fencing token: " + lease.getLockName()))
                    .lockName(lease.getLockName())
                    .lockKey(lease.getLockKey())
                    .ownerToken(lease.getOwnerToken())
                    .fencingToken(lease.fencingToken().orElseThrow())
                    .fencingTokenProviderName(lease.fencingTokenProviderName().orElse(null))
                    .waitDuration(waitDuration)
                    .build();
        }

        Throwable error = response.getError();
        if (error == null) {
            error = new LockProviderException(response.getMessage() == null
                    ? "failed to verify lock ownership after external fencing"
                    : response.getMessage());
        }
        releaseAfterPreparationFailure(lockProvider, lease, error);
        eventPublisher.publish(eventFactory.fromLease(lease,
                LockEventType.PROVIDER_ERROR, LockStage.CHECK, LockStatus.PROVIDER_ERROR, error));
        return preparationCheckFailure(lease, waitDuration, error);
    }

    private LockResult<LockHandle> preparationCheckFailure(
            LockLease lease, Duration waitDuration, Throwable error
    ) {
        return LockResult.<LockHandle>builder()
                .status(LockStatus.PROVIDER_ERROR)
                .stage(LockStage.CHECK)
                .acquired(true)
                .error(error)
                .lockName(lease.getLockName())
                .lockKey(lease.getLockKey())
                .ownerToken(lease.getOwnerToken())
                .fencingToken(lease.fencingToken().isPresent()
                        ? lease.fencingToken().getAsLong() : null)
                .fencingTokenProviderName(lease.fencingTokenProviderName().orElse(null))
                .waitDuration(waitDuration)
                .build();
    }

    /** 发号或发号后校验失败时，尽最大努力释放已经获取的锁。 */
    private void releaseAfterPreparationFailure(
            LockProvider lockProvider, LockLease lease, Throwable primaryError
    ) {
        try {
            LockReleaseResponse releaseResponse = lockProvider.release(LockReleaseRequest.fromLease(lease));
            switch (releaseResponse.getStatus()) {
                case RELEASED:
                    metricsFacade.recordRelease(lease, true);
                    eventPublisher.publish(eventFactory.fromLease(lease,
                            LockEventType.RELEASED, LockStage.RELEASE, null, null));
                    break;
                case NOT_FOUND:
                case NOT_OWNER:
                    metricsFacade.recordRelease(lease, false);
                    metricsFacade.recordLost(lease);
                    eventPublisher.publish(eventFactory.fromLease(lease,
                            LockEventType.LOCK_LOST, LockStage.RELEASE, LockStatus.LOCK_LOST, null));
                    break;
                case PROVIDER_ERROR:
                default:
                    metricsFacade.recordRelease(lease, false);
                    Throwable releaseError = releaseResponse.getError();
                    if (releaseError == null) {
                        releaseError = new LockProviderException(
                                releaseResponse.getMessage() == null
                                        ? "failed to release lock after preparation failure"
                                        : releaseResponse.getMessage());
                    }
                    primaryError.addSuppressed(releaseError);
                    eventPublisher.publish(eventFactory.fromLease(lease,
                            LockEventType.RELEASE_FAILED, LockStage.RELEASE,
                            LockStatus.RELEASE_FAILED, releaseError));
                    break;
            }
        } catch (Throwable releaseError) {
            metricsFacade.recordRelease(lease, false);
            primaryError.addSuppressed(releaseError);
            eventPublisher.publish(eventFactory.fromLease(lease,
                    LockEventType.RELEASE_FAILED, LockStage.RELEASE,
                    LockStatus.RELEASE_FAILED, releaseError));
        }
    }

    @Override
    public <T> LockResult<T> execute(String lockName, LockOptions options, LockCallback<T> callback) {
        Objects.requireNonNull(callback, "callback must not be null");
        LockResult<LockHandle> acquireResult = tryLock(lockName, options);
        if (acquireResult.status() != LockStatus.ACQUIRED) {
            return copyAcquireFailure(acquireResult);
        }

        DefaultLockHandle handle = (DefaultLockHandle) acquireResult.handle()
                .orElseThrow(() -> new IllegalStateException("acquired result has no handle"));
        LockOptions actualOptions = defaultOptions(options);
        if (actualOptions.isAutoRenew()) {
            watchdog.start(handle, actualOptions);
        }

        ExecutionOutcome<T> executionOutcome;
        try {
            eventPublisher.publish(eventFactory.fromLease(handle.lease(),
                    LockEventType.EXECUTION_STARTED, LockStage.EXECUTE, null, null));
            T value = callback.doWithLock(handle);
            if (handle.isLost() && actualOptions.isFailOnLockLost()) {
                executionOutcome = ExecutionOutcome.failure(LockStatus.LOCK_LOST, LockStage.RENEW,
                        new LockLostException("lock lost during execution: " + lockName));
            } else {
                executionOutcome = ExecutionOutcome.success(value);
            }
        } catch (FencingTokenRejectedException error) {
            executionOutcome = ExecutionOutcome.failure(
                    LockStatus.FENCING_REJECTED, LockStage.FENCING, error);
            eventPublisher.publish(eventFactory.fromLease(handle.lease(),
                    LockEventType.FENCING_REJECTED, LockStage.FENCING,
                    LockStatus.FENCING_REJECTED, error));
        } catch (LockLostException error) {
            executionOutcome = ExecutionOutcome.failure(LockStatus.LOCK_LOST, LockStage.CHECK, error);
            eventPublisher.publish(eventFactory.fromLease(handle.lease(),
                    LockEventType.LOCK_LOST, LockStage.CHECK, LockStatus.LOCK_LOST, error));
        } catch (Throwable error) {
            executionOutcome = ExecutionOutcome.failure(
                    LockStatus.EXECUTION_FAILED, LockStage.EXECUTE, error);
            eventPublisher.publish(eventFactory.fromLease(handle.lease(),
                    LockEventType.EXECUTION_FAILED, LockStage.EXECUTE,
                    LockStatus.EXECUTION_FAILED, error));
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
            eventPublisher.publish(eventFactory.fromLease(handle.lease(),
                    LockEventType.EXECUTION_SUCCESS, LockStage.EXECUTE, LockStatus.SUCCESS, null));
        }
        return result;
    }

    private LockOptions validate(String lockName, LockOptions options) {
        lockNameValidator.validate(lockName);
        LockOptions actualOptions = defaultOptions(options);
        actualOptions.validate();
        return actualOptions;
    }

    private LockOptions defaultOptions(LockOptions options) {
        return options == null ? defaultOptions : options;
    }

    private LockProvider selectProvider(LockOptions options) {
        return providerRegistry.getProvider(options.getProviderName());
    }

    private void validateProviderCapabilities(LockProvider provider, LockOptions options, FencingTokenPlan fencingPlan) {
        if (options.isAutoRenew() && !provider.capabilities().isAutoRenewSupported()) {
            throw new IllegalArgumentException(
                    "provider does not support auto renew: " + provider.providerName());
        }
        if (fencingPlan.isNative() && !provider.capabilities().isFencingTokenSupported()) {
            throw new IllegalArgumentException(
                    "provider does not support native fencing token: " + provider.providerName());
        }
    }

    private DefaultLockHandle newHandle(LockProvider provider, LockLease lease) {
        return new DefaultLockHandle(provider, lease, new LockRuntimeState(),
                eventPublisher, eventFactory, metricsFacade);
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
