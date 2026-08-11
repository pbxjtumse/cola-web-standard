package com.xjtu.iron.idempotent.core;

import com.xjtu.iron.distributed.lock.api.DistributedLockClient;
import com.xjtu.iron.distributed.lock.api.LockCallback;
import com.xjtu.iron.distributed.lock.api.LockOptions;
import com.xjtu.iron.distributed.lock.api.LockResult;
import com.xjtu.iron.distributed.lock.api.LockWaitStrategy;
import com.xjtu.iron.idempotent.api.IdempotencyCallback;
import com.xjtu.iron.idempotent.api.IdempotencyContext;
import com.xjtu.iron.idempotent.api.IdempotencyExecutor;
import com.xjtu.iron.idempotent.api.IdempotencyLockOptions;
import com.xjtu.iron.idempotent.api.IdempotencyOptions;
import com.xjtu.iron.idempotent.api.IdempotencyRequest;
import com.xjtu.iron.idempotent.api.IdempotencyResult;
import com.xjtu.iron.idempotent.api.IdempotencyResultStatus;
import com.xjtu.iron.idempotent.api.IdempotencyStage;
import com.xjtu.iron.idempotent.api.repository.IdempotencyAcquireRequest;
import com.xjtu.iron.idempotent.api.repository.IdempotencyAcquireResult;
import com.xjtu.iron.idempotent.api.repository.IdempotencyFailureInfo;
import com.xjtu.iron.idempotent.api.repository.IdempotencyFailureRequest;
import com.xjtu.iron.idempotent.api.repository.IdempotencyRecord;
import com.xjtu.iron.idempotent.api.repository.IdempotencyRepository;
import com.xjtu.iron.idempotent.api.repository.IdempotencySuccessRequest;
import com.xjtu.iron.idempotent.api.repository.IdempotencyWriteResult;
import com.xjtu.iron.idempotent.api.repository.IdempotencyWriteStatus;
import com.xjtu.iron.idempotent.api.spi.IdempotencyFailureClassifier;
import com.xjtu.iron.idempotent.api.spi.IdempotencyResultCodec;
import com.xjtu.iron.idempotent.core.observation.IdempotencyEvent;
import com.xjtu.iron.idempotent.core.observation.IdempotencyEventPublisher;
import com.xjtu.iron.idempotent.core.observation.IdempotencyEventType;
import com.xjtu.iron.idempotent.core.observation.IdempotencyMetrics;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 默认幂等执行器。
 *
 * <p>主流程保持为四个清晰阶段：</p>
 * <ol>
 *     <li>校验请求并选择 Repository；</li>
 *     <li>原子抢占 Idempotency State；</li>
 *     <li>只有 ACQUIRED owner 执行业务 callback；</li>
 *     <li>通过 ownerToken + version 条件写入 SUCCESS / FAILED。</li>
 * </ol>
 *
 * <p><strong>最重要的边界：</strong>DistributedLockClient 只保护 Repository.tryAcquire 的短临界区，
 * callback 始终在锁外执行。Repository 的 UNIQUE / Lua / CAS 才是幂等正确性的根基。</p>
 */
public final class DefaultIdempotencyExecutor implements IdempotencyExecutor {

    private final IdempotencyRepositoryRegistry registry;
    private final IdempotencyDefaults defaults;
    private final IdempotencyOwnerTokenGenerator ownerGenerator;
    private final IdempotencyFailureClassifier failureClassifier;
    private final IdempotencyResultCodec codec;
    private final DistributedLockClient lockClient;
    private final IdempotencyEventPublisher events;
    private final IdempotencyMetrics metrics;
    private final Clock clock;

    public DefaultIdempotencyExecutor(
            IdempotencyRepositoryRegistry registry,
            IdempotencyDefaults defaults,
            IdempotencyOwnerTokenGenerator ownerGenerator,
            IdempotencyFailureClassifier failureClassifier,
            IdempotencyResultCodec codec,
            DistributedLockClient lockClient,
            IdempotencyEventPublisher events,
            IdempotencyMetrics metrics,
            Clock clock) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.defaults = Objects.requireNonNull(defaults, "defaults must not be null");
        this.ownerGenerator = Objects.requireNonNull(ownerGenerator, "ownerGenerator must not be null");
        this.failureClassifier = Objects.requireNonNull(
                failureClassifier, "failureClassifier must not be null");
        this.codec = codec;
        this.lockClient = lockClient;
        this.events = events == null ? IdempotencyEventPublisher.noop() : events;
        this.metrics = metrics == null ? IdempotencyMetrics.noop() : metrics;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    public <T> IdempotencyResult<T> execute(
            IdempotencyRequest request,
            Class<T> resultType,
            IdempotencyCallback<T> callback) {

        Instant startedAt = Instant.now(clock);
        Objects.requireNonNull(callback, "callback must not be null");

        PreparedExecution prepared;
        try {
            prepared = prepare(request, resultType);
        } catch (RuntimeException error) {
            return IdempotencyResult.<T>builder()
                    .status(IdempotencyResultStatus.INVALID_OPTIONS)
                    .stage(IdempotencyStage.VALIDATE)
                    .error(error)
                    .build();
        }

        IdempotencyOptions options = prepared.options;
        IdempotencyRepository repository = prepared.repository;

        String ownerToken = ownerGenerator.generate(options.getNamespace(), request.getKey());
        IdempotencyAcquireRequest acquireRequest = new IdempotencyAcquireRequest(
                options.getNamespace(),
                request.getKey(),
                normalize(request.getRequestHash()),
                ownerToken,
                options.getMode(),
                options.getProcessingTimeout(),
                options.getRecordTtl(),
                options.isRetryOnProcessingTimeout(),
                options.isRetryFailed(),
                Instant.now(clock));

        publish(
                IdempotencyEventType.ACQUIRE_ATTEMPT,
                IdempotencyStage.ACQUIRE_STATE,
                options,
                repository,
                null);

        AcquireInvocation invocation = acquireState(repository, acquireRequest, options);
        if (invocation.lockRejected) {
            return IdempotencyResult.<T>builder()
                    .status(IdempotencyResultStatus.LOCK_NOT_ACQUIRED)
                    .stage(IdempotencyStage.LOCK)
                    .error(invocation.error)
                    .build();
        }

        IdempotencyAcquireResult acquireResult = invocation.result;
        metrics.recordAcquire(
                options.getMode(),
                repository.providerName(),
                acquireResult.getStatus().name());

        return switch (acquireResult.getStatus()) {
            case SUCCESS -> {
                publish(IdempotencyEventType.REPLAYED, IdempotencyStage.REPLAY,
                        options, repository, null);
                yield replay(acquireResult.getRecord(), resultType, invocation.lockFallback);
            }
            case PROCESSING -> {
                publish(IdempotencyEventType.PROCESSING, IdempotencyStage.ACQUIRE_STATE,
                        options, repository, null);
                yield simple(
                        IdempotencyResultStatus.PROCESSING,
                        IdempotencyStage.ACQUIRE_STATE,
                        acquireResult.getRecord(),
                        null,
                        invocation.lockFallback);
            }
            case FAILED -> {
                publish(IdempotencyEventType.PREVIOUS_FAILED, IdempotencyStage.ACQUIRE_STATE,
                        options, repository, null);
                yield simple(
                        IdempotencyResultStatus.PREVIOUS_FAILED,
                        IdempotencyStage.ACQUIRE_STATE,
                        acquireResult.getRecord(),
                        null,
                        invocation.lockFallback);
            }
            case KEY_CONFLICT -> {
                publish(IdempotencyEventType.KEY_CONFLICT, IdempotencyStage.ACQUIRE_STATE,
                        options, repository, null);
                yield simple(
                        IdempotencyResultStatus.KEY_CONFLICT,
                        IdempotencyStage.ACQUIRE_STATE,
                        acquireResult.getRecord(),
                        null,
                        invocation.lockFallback);
            }
            case PROVIDER_ERROR -> {
                publish(IdempotencyEventType.REPOSITORY_ERROR, IdempotencyStage.ACQUIRE_STATE,
                        options, repository, acquireResult.getError());
                yield simple(
                        IdempotencyResultStatus.REPOSITORY_ERROR,
                        IdempotencyStage.ACQUIRE_STATE,
                        acquireResult.getRecord(),
                        acquireResult.getError(),
                        invocation.lockFallback);
            }
            case ACQUIRED -> {
                publish(IdempotencyEventType.ACQUIRED, IdempotencyStage.ACQUIRE_STATE,
                        options, repository, null);
                yield executeOwned(
                        request,
                        options,
                        repository,
                        acquireResult.getRecord(),
                        callback,
                        startedAt,
                        invocation.lockFallback);
            }
        };
    }

    /**
     * 请求级准备工作。这里不访问外部存储，因此失败统一归为 VALIDATE。
     */
    private PreparedExecution prepare(IdempotencyRequest request, Class<?> resultType) {
        validateRequest(request);

        IdempotencyOptions options = request.getOptions() == null
                ? defaults.defaultOptions()
                : request.getOptions();
        options.validate();

        if (options.isStoreResult() && codec == null) {
            throw new IllegalArgumentException("storeResult=true requires IdempotencyResultCodec");
        }
        if (options.isStoreResult() && resultType == null) {
            throw new IllegalArgumentException(
                    "storeResult=true requires resultType so replay can safely deserialize the stored result");
        }

        IdempotencyRepository repository = registry.resolve(
                options.getMode(), options.getRepositoryName());
        return new PreparedExecution(options, repository);
    }

    /**
     * 真正持有 PROCESSING 执行权的 owner 才会进入这里。
     */
    private <T> IdempotencyResult<T> executeOwned(
            IdempotencyRequest request,
            IdempotencyOptions options,
            IdempotencyRepository repository,
            IdempotencyRecord record,
            IdempotencyCallback<T> callback,
            Instant startedAt,
            boolean lockFallback) {

        IdempotencyContext context = new IdempotencyContext(
                options.getNamespace(),
                request.getKey(),
                record.getOwnerToken(),
                record.getVersion(),
                options.getMode(),
                Instant.now(clock),
                record.getProcessingExpireAt());

        publish(
                IdempotencyEventType.EXECUTION_STARTED,
                IdempotencyStage.EXECUTE,
                options,
                repository,
                null);

        try {
            T value = callback.doWithIdempotency(context);
            String payload = encodeResultIfNecessary(
                    value, request, options, repository, record);

            // encodeResultIfNecessary 发生异常时会抛 ResultEncodeException，统一进入下面 catch。
            IdempotencyWriteResult writeResult = repository.markSuccess(
                    new IdempotencySuccessRequest(
                            options.getNamespace(),
                            request.getKey(),
                            record.getOwnerToken(),
                            record.getVersion(),
                            payload,
                            options.getRecordTtl(),
                            Instant.now(clock)));

            if (writeResult.getStatus() == IdempotencyWriteStatus.UPDATED) {
                publish(
                        IdempotencyEventType.EXECUTION_SUCCESS,
                        IdempotencyStage.COMPLETE_STATE,
                        options,
                        repository,
                        null);
                return finish(
                        IdempotencyResultStatus.EXECUTED,
                        IdempotencyStage.COMPLETE_STATE,
                        value,
                        writeResult.getRecord(),
                        null,
                        options,
                        repository,
                        startedAt,
                        lockFallback);
            }

            if (writeResult.getStatus() == IdempotencyWriteStatus.STALE_OWNER
                    || writeResult.getStatus() == IdempotencyWriteStatus.ALREADY_FINAL) {
                publish(
                        IdempotencyEventType.OWNERSHIP_LOST,
                        IdempotencyStage.COMPLETE_STATE,
                        options,
                        repository,
                        null);
                return finish(
                        IdempotencyResultStatus.OWNERSHIP_LOST,
                        IdempotencyStage.COMPLETE_STATE,
                        value,
                        writeResult.getRecord(),
                        null,
                        options,
                        repository,
                        startedAt,
                        lockFallback);
            }

            Throwable error = writeResult.getError() != null
                    ? writeResult.getError()
                    : new IllegalStateException("cannot mark SUCCESS: " + writeResult.getStatus());
            publish(
                    IdempotencyEventType.REPOSITORY_ERROR,
                    IdempotencyStage.COMPLETE_STATE,
                    options,
                    repository,
                    error);
            return finish(
                    IdempotencyResultStatus.REPOSITORY_ERROR,
                    IdempotencyStage.COMPLETE_STATE,
                    value,
                    writeResult.getRecord(),
                    error,
                    options,
                    repository,
                    startedAt,
                    lockFallback);

        } catch (ResultEncodeException encodeError) {
            return finish(
                    IdempotencyResultStatus.RESULT_CODEC_ERROR,
                    IdempotencyStage.COMPLETE_STATE,
                    null,
                    encodeError.record,
                    encodeError.getCause(),
                    options,
                    repository,
                    startedAt,
                    lockFallback);
        } catch (Exception businessError) {
            return handleBusinessFailure(
                    request,
                    options,
                    repository,
                    record,
                    businessError,
                    startedAt,
                    lockFallback);
        }
    }

    /**
     * 结果快照默认关闭。开启后，编码失败会把当前 PROCESSING 显式落为不可重试 FAILED，
     * 避免业务已经成功返回但幂等记录长期卡在 PROCESSING。
     */
    private <T> String encodeResultIfNecessary(
            T value,
            IdempotencyRequest request,
            IdempotencyOptions options,
            IdempotencyRepository repository,
            IdempotencyRecord record) throws ResultEncodeException {

        if (!options.isStoreResult()) {
            return null;
        }

        try {
            return codec.encode(value);
        } catch (Exception codecError) {
            Instant failedAt = Instant.now(clock);
            IdempotencyFailureInfo failure = new IdempotencyFailureInfo(
                    "RESULT_ENCODE_FAILED",
                    codecError.getMessage(),
                    false,
                    failedAt);

            IdempotencyWriteResult writeResult = repository.markFailed(
                    new IdempotencyFailureRequest(
                            options.getNamespace(),
                            request.getKey(),
                            record.getOwnerToken(),
                            record.getVersion(),
                            failure,
                            options.getRecordTtl(),
                            failedAt));

            IdempotencyRecord resultRecord = writeResult.getRecord() == null
                    ? record : writeResult.getRecord();
            throw new ResultEncodeException(codecError, resultRecord);
        }
    }

    private <T> IdempotencyResult<T> handleBusinessFailure(
            IdempotencyRequest request,
            IdempotencyOptions options,
            IdempotencyRepository repository,
            IdempotencyRecord record,
            Exception businessError,
            Instant startedAt,
            boolean lockFallback) {

        if (businessError instanceof InterruptedException) {
            // 不能吞掉线程中断信号，否则上层线程池/取消机制无法感知。
            Thread.currentThread().interrupt();
        }

        Instant failedAt = Instant.now(clock);
        IdempotencyFailureInfo failure = failureClassifier.classify(businessError, failedAt);
        IdempotencyWriteResult writeResult = repository.markFailed(
                new IdempotencyFailureRequest(
                        options.getNamespace(),
                        request.getKey(),
                        record.getOwnerToken(),
                        record.getVersion(),
                        failure,
                        options.getRecordTtl(),
                        failedAt));

        if (writeResult.getStatus() == IdempotencyWriteStatus.PROVIDER_ERROR
                && writeResult.getError() != null) {
            businessError.addSuppressed(writeResult.getError());
        }

        publish(
                IdempotencyEventType.EXECUTION_FAILED,
                IdempotencyStage.EXECUTE,
                options,
                repository,
                businessError);

        return finish(
                IdempotencyResultStatus.EXECUTION_FAILED,
                IdempotencyStage.EXECUTE,
                null,
                writeResult.getRecord() == null ? record : writeResult.getRecord(),
                businessError,
                options,
                repository,
                startedAt,
                lockFallback);
    }

    /**
     * 可选分布式锁只减少同 key 同时打到 Repository 的竞争。
     *
     * <p>如果 fallbackToStateOnFailure=true，锁不可用时仍直接调用 Repository.tryAcquire。
     * 这反向验证了 Repository 必须独立正确，而不能依赖锁。</p>
     */
    private AcquireInvocation acquireState(
            IdempotencyRepository repository,
            IdempotencyAcquireRequest request,
            IdempotencyOptions options) {

        IdempotencyLockOptions lock = options.getLockOptions();
        if (!lock.isEnabled()) {
            return AcquireInvocation.direct(repository.tryAcquire(request));
        }

        if (lockClient == null) {
            if (lock.isFallbackToStateOnFailure()) {
                publish(IdempotencyEventType.LOCK_FALLBACK, IdempotencyStage.LOCK,
                        options, repository, null);
                return AcquireInvocation.fallback(repository.tryAcquire(request));
            }
            return AcquireInvocation.lockRejected(
                    new IllegalStateException("lock enabled but DistributedLockClient unavailable"));
        }

        LockWaitStrategy waitStrategy = lock.getWaitTime().isZero()
                ? LockWaitStrategy.NO_WAIT
                : LockWaitStrategy.BACKOFF;

        LockOptions lockOptions = LockOptions.builder()
                .namespace("idempotency:" + options.getNamespace())
                .providerName(lock.getProviderName())
                .waitTime(lock.getWaitTime())
                .waitStrategy(waitStrategy)
                .leaseTime(lock.getLeaseTime())
                .autoRenew(false)
                .fencingRequired(false)
                .build();

        LockResult<IdempotencyAcquireResult> lockResult = lockClient.execute(
                "state:" + request.getKey(),
                lockOptions,
                (LockCallback<IdempotencyAcquireResult>) handle -> repository.tryAcquire(request));

        if (lockResult.isSuccess() && lockResult.value().isPresent()) {
            return AcquireInvocation.direct(lockResult.value().get());
        }

        if (lock.isFallbackToStateOnFailure()) {
            publish(
                    IdempotencyEventType.LOCK_FALLBACK,
                    IdempotencyStage.LOCK,
                    options,
                    repository,
                    lockResult.error().orElse(null));
            return AcquireInvocation.fallback(repository.tryAcquire(request));
        }

        return AcquireInvocation.lockRejected(
                lockResult.error().orElseGet(() -> new IllegalStateException(
                        "distributed lock not acquired: " + lockResult.status())));
    }

    private <T> IdempotencyResult<T> replay(
            IdempotencyRecord record,
            Class<T> resultType,
            boolean lockFallback) {

        if (record == null
                || record.getResultPayload() == null
                || record.getResultPayload().isBlank()) {
            return IdempotencyResult.<T>builder()
                    .status(IdempotencyResultStatus.REPLAYED)
                    .stage(IdempotencyStage.REPLAY)
                    .record(record)
                    .lockFallback(lockFallback)
                    .build();
        }

        if (codec == null || resultType == null) {
            return IdempotencyResult.<T>builder()
                    .status(IdempotencyResultStatus.RESULT_CODEC_ERROR)
                    .stage(IdempotencyStage.REPLAY)
                    .record(record)
                    .error(new IllegalStateException(
                            "stored result exists but resultType/codec missing"))
                    .lockFallback(lockFallback)
                    .build();
        }

        try {
            return IdempotencyResult.<T>builder()
                    .status(IdempotencyResultStatus.REPLAYED)
                    .stage(IdempotencyStage.REPLAY)
                    .value(codec.decode(record.getResultPayload(), resultType))
                    .record(record)
                    .lockFallback(lockFallback)
                    .build();
        } catch (Exception error) {
            return IdempotencyResult.<T>builder()
                    .status(IdempotencyResultStatus.RESULT_CODEC_ERROR)
                    .stage(IdempotencyStage.REPLAY)
                    .record(record)
                    .error(error)
                    .lockFallback(lockFallback)
                    .build();
        }
    }

    private <T> IdempotencyResult<T> simple(
            IdempotencyResultStatus status,
            IdempotencyStage stage,
            IdempotencyRecord record,
            Throwable error,
            boolean lockFallback) {
        return IdempotencyResult.<T>builder()
                .status(status)
                .stage(stage)
                .record(record)
                .error(error)
                .lockFallback(lockFallback)
                .build();
    }

    private <T> IdempotencyResult<T> finish(
            IdempotencyResultStatus status,
            IdempotencyStage stage,
            T value,
            IdempotencyRecord record,
            Throwable error,
            IdempotencyOptions options,
            IdempotencyRepository repository,
            Instant startedAt,
            boolean lockFallback) {

        metrics.recordExecution(
                options.getMode(),
                repository.providerName(),
                status,
                Duration.between(startedAt, Instant.now(clock)));

        return IdempotencyResult.<T>builder()
                .status(status)
                .stage(stage)
                .value(value)
                .record(record)
                .error(error)
                .lockFallback(lockFallback)
                .build();
    }

    private void validateRequest(IdempotencyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.getKey() == null || request.getKey().isBlank()) {
            throw new IllegalArgumentException("idempotency key must not be blank");
        }
    }

    private void publish(
            IdempotencyEventType type,
            IdempotencyStage stage,
            IdempotencyOptions options,
            IdempotencyRepository repository,
            Throwable error) {
        events.publish(new IdempotencyEvent(
                type,
                stage,
                options.getMode(),
                repository.providerName(),
                Instant.now(clock),
                error));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /** 只在 execute 内部使用，避免为一个小型流程对象额外创建公共类。 */
    private static final class PreparedExecution {
        private final IdempotencyOptions options;
        private final IdempotencyRepository repository;

        private PreparedExecution(IdempotencyOptions options, IdempotencyRepository repository) {
            this.options = options;
            this.repository = repository;
        }
    }

    /** 分布式锁包装 tryAcquire 后的内部结果。 */
    private static final class AcquireInvocation {
        private final IdempotencyAcquireResult result;
        private final boolean lockFallback;
        private final boolean lockRejected;
        private final Throwable error;

        private AcquireInvocation(
                IdempotencyAcquireResult result,
                boolean lockFallback,
                boolean lockRejected,
                Throwable error) {
            this.result = result;
            this.lockFallback = lockFallback;
            this.lockRejected = lockRejected;
            this.error = error;
        }

        private static AcquireInvocation direct(IdempotencyAcquireResult result) {
            return new AcquireInvocation(result, false, false, null);
        }

        private static AcquireInvocation fallback(IdempotencyAcquireResult result) {
            return new AcquireInvocation(result, true, false, null);
        }

        private static AcquireInvocation lockRejected(Throwable error) {
            return new AcquireInvocation(null, false, true, error);
        }
    }

    /**
     * 仅用于把 result codec 失败从业务 callback 异常中区分出来。
     * 这是 Core 内部控制流异常，不暴露给 API。
     */
    private static final class ResultEncodeException extends Exception {
        private final IdempotencyRecord record;

        private ResultEncodeException(Throwable cause, IdempotencyRecord record) {
            super(cause);
            this.record = record;
        }
    }
}
