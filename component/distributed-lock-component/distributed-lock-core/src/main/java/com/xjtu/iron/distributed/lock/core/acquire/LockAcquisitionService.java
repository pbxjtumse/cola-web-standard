package com.xjtu.iron.distributed.lock.core.acquire;

import com.xjtu.iron.distributed.lock.api.model.LockHandle;
import com.xjtu.iron.distributed.lock.api.model.LockOptions;
import com.xjtu.iron.distributed.lock.api.LockWaitStrategy;
import com.xjtu.iron.distributed.lock.api.model.LockResult;
import com.xjtu.iron.distributed.lock.api.status.LockStage;
import com.xjtu.iron.distributed.lock.api.status.LockStatus;
import com.xjtu.iron.distributed.lock.api.exception.InvalidLockOptionsException;
import com.xjtu.iron.distributed.lock.core.observability.LockEventFactory;
import com.xjtu.iron.distributed.lock.core.observability.LockEventPublisher;
import com.xjtu.iron.distributed.lock.core.observability.LockEventType;
import com.xjtu.iron.distributed.lock.core.fencing.FencingTokenCoordinator;
import com.xjtu.iron.distributed.lock.core.fencing.FencingTokenPlan;
import com.xjtu.iron.distributed.lock.core.spi.protocol.LockAcquireRequest;
import com.xjtu.iron.distributed.lock.core.support.LockNameValidator;
import com.xjtu.iron.distributed.lock.core.spi.LockProvider;
import com.xjtu.iron.distributed.lock.core.spi.LockProviderRegistry;
import com.xjtu.iron.distributed.lock.core.spi.protocol.LockAcquireResponse;
import com.xjtu.iron.distributed.lock.core.support.OwnerTokenGenerator;
import com.xjtu.iron.distributed.lock.core.wait.LockWaitContext;
import com.xjtu.iron.distributed.lock.core.wait.LockWaiter;
import com.xjtu.iron.distributed.lock.core.wait.LockWaiterFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 分布式锁获取流程。
 *
 * <p>本类集中负责一次 acquire 的完整编排：解析并校验参数、选择 Provider、规划 fencing、
 * 生成 ownerToken、调用等待策略以及把 Provider 响应交给状态处理器。短小的 request/context
 * 组装保留在流程内，避免为了 builder 再制造一组 Factory 类。</p>
 */
public final class LockAcquisitionService {

    private final LockProviderRegistry providerRegistry;
    private final OwnerTokenGenerator ownerTokenGenerator;
    private final LockWaiterFactory waiterFactory;
    private final LockEventPublisher eventPublisher;
    private final LockEventFactory eventFactory;
    private final LockNameValidator lockNameValidator;
    private final LockOptions defaultOptions;
    private final Clock clock;
    private final FencingTokenCoordinator fencingTokenCoordinator;
    private final LockAcquireOutcomeHandlerRegistry outcomeHandlerRegistry;

    public LockAcquisitionService(LockProviderRegistry providerRegistry, OwnerTokenGenerator ownerTokenGenerator, LockWaiterFactory waiterFactory,
            LockEventPublisher eventPublisher, LockEventFactory eventFactory, LockNameValidator lockNameValidator, LockOptions defaultOptions,
            Clock clock, FencingTokenCoordinator fencingTokenCoordinator, LockAcquireOutcomeHandlerRegistry outcomeHandlerRegistry) {
        this.providerRegistry = Objects.requireNonNull(providerRegistry, "providerRegistry must not be null");
        this.ownerTokenGenerator = Objects.requireNonNull(ownerTokenGenerator, "ownerTokenGenerator must not be null");
        this.waiterFactory = Objects.requireNonNull(waiterFactory, "waiterFactory must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.eventFactory = Objects.requireNonNull(eventFactory, "eventFactory must not be null");
        this.lockNameValidator = Objects.requireNonNull(lockNameValidator, "lockNameValidator must not be null");
        this.defaultOptions = defaultOptions == null ? LockOptions.defaults() : defaultOptions;
        this.defaultOptions.validate();
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.fencingTokenCoordinator = Objects.requireNonNull(fencingTokenCoordinator, "fencingTokenCoordinator must not be null");
        this.outcomeHandlerRegistry = Objects.requireNonNull(outcomeHandlerRegistry, "outcomeHandlerRegistry must not be null");
    }

    /** 返回公开 tryLock 所需的最终结果。 */
    public LockResult<LockHandle> tryLock(String lockName, LockOptions options) {
        return acquire(lockName, options).result();
    }

    /**
     * 获取锁，并额外保留本次真正生效的 LockOptions，供 execute 流程复用。
     */
    public AcquireAttempt acquire(String lockName, LockOptions options) {
        Instant operationStart = Instant.now(clock);
        try {
            LockOptions actualOptions = resolveAndValidate(lockName, options);
            LockProvider provider = providerRegistry.getProvider(actualOptions.getProviderName());
            FencingTokenPlan fencingPlan = fencingTokenCoordinator.plan(provider, actualOptions);
            validateProviderCapabilities(provider, actualOptions);

            String ownerToken = ownerTokenGenerator.generate(actualOptions.getNamespace(), lockName);
            LockAcquireRequest request = LockAcquireRequest.builder()
                    .lockName(lockName)
                    .ownerToken(ownerToken)
                    .options(actualOptions)
                    .nativeFencingRequired(fencingPlan.isNative())
                    .build();

            eventPublisher.publish(eventFactory.fromAcquireRequest(provider, request, LockEventType.ACQUIRE_ATTEMPT, LockStage.ACQUIRE, null, null));

            LockWaiter waiter = waiterFactory.getWaiter(actualOptions.getWaitStrategy());
            Instant acquireStart = Instant.now(clock);
            LockAcquireResponse response = waiter.waitForLock(new LockWaitContext(request, provider, clock));
            Duration waitDuration = Duration.between(acquireStart, Instant.now(clock));

            LockAcquireOutcomeContext outcomeContext = LockAcquireOutcomeContext.builder()
                    .lockName(lockName)
                    .provider(provider)
                    .options(actualOptions)
                    .request(request)
                    .response(response)
                    .fencingPlan(fencingPlan)
                    .waitDuration(waitDuration)
                    .build();

            return AcquireAttempt.completed(outcomeHandlerRegistry.handle(outcomeContext), actualOptions);
        } catch (IllegalArgumentException | InvalidLockOptionsException error) {
            return AcquireAttempt.invalid(invalidOptionsResult(lockName, error, operationStart));
        }
    }

    private LockOptions resolveAndValidate(String lockName, LockOptions options) {
        lockNameValidator.validate(lockName);
        LockOptions actualOptions = options == null ? defaultOptions : options;
        actualOptions.validate();
        return actualOptions;
    }

    private void validateProviderCapabilities(LockProvider provider, LockOptions options) {
        if (options.isAutoRenew() && !provider.capabilities().isAutoRenewSupported()) {
            throw new IllegalArgumentException("provider does not support auto renew: " + provider.providerName());
        }
        if (options.getWaitStrategy() == LockWaitStrategy.PROVIDER_NATIVE && !provider.capabilities().isNativeWaitSupported()) {
            throw new IllegalArgumentException("provider does not support provider-native waiting: " + provider.providerName());
        }

        // 通用能力校验之后，再给具体 Provider 一次校验自身约束的机会。
        provider.validateOptions(options);
    }

    private LockResult<LockHandle> invalidOptionsResult(String lockName, RuntimeException error, Instant operationStart) {
        return LockResult.<LockHandle>builder()
                .status(LockStatus.INVALID_OPTIONS)
                .stage(LockStage.VALIDATE)
                .acquired(false)
                .error(error)
                .lockName(lockName)
                .waitDuration(Duration.between(operationStart, Instant.now(clock)))
                .build();
    }

    /** acquire 的内部结果；避免 execute 再次解析默认 LockOptions。 */
    public static final class AcquireAttempt {

        private final LockResult<LockHandle> result;
        private final LockOptions options;

        private AcquireAttempt(LockResult<LockHandle> result, LockOptions options) {
            this.result = Objects.requireNonNull(result, "result must not be null");
            this.options = options;
        }

        public static AcquireAttempt completed(LockResult<LockHandle> result, LockOptions options) {
            return new AcquireAttempt(result, Objects.requireNonNull(options, "options must not be null"));
        }

        public static AcquireAttempt invalid(LockResult<LockHandle> result) {
            return new AcquireAttempt(result, null);
        }

        public LockResult<LockHandle> result() {
            return result;
        }

        public LockOptions requireOptions() {
            if (options == null) {
                throw new IllegalStateException("acquire attempt has no resolved options");
            }
            return options;
        }
    }
}
