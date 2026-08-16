package com.xjtu.iron.idempotent.core;

import com.xjtu.iron.distributed.lock.api.DistributedLockClient;
import com.xjtu.iron.distributed.lock.api.LockCallback;
import com.xjtu.iron.distributed.lock.api.LockOptions;
import com.xjtu.iron.distributed.lock.api.LockResult;
import com.xjtu.iron.distributed.lock.api.LockWaitStrategy;
import com.xjtu.iron.idempotent.api.*;
import com.xjtu.iron.idempotent.api.repository.*;
import com.xjtu.iron.idempotent.api.spi.IdempotencyFailureClassifier;
import com.xjtu.iron.idempotent.api.spi.IdempotencyResultCodec;
import com.xjtu.iron.idempotent.core.observation.*;
import com.xjtu.iron.idempotent.core.transaction.IdempotencyTransactionCoordinator;
import com.xjtu.iron.idempotent.core.transaction.IdempotencyTransactionException;
import com.xjtu.iron.idempotent.core.transaction.IdempotencyTransactionOutcome;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 默认幂等执行器。
 *
 * <p>V1.2 在 V1.1 状态机基础上正式接入 transaction-component：</p>
 * <ul>
 *     <li>execute()：普通 API / 消息消费路径，只判断 PROCESSING 是否已超时，不自动接管；</li>
 *     <li>recover()：由外部 Reliable Task 调用，原子接管超时 PROCESSING 或可恢复 FAILED。</li>
 * </ul>
 *
 * <p><strong>重要边界：</strong>DistributedLockClient 只包住 Repository 的极短状态抢占，
 * callback 永远在分布式锁外执行。Repository 的 UNIQUE / Lua / CAS 才是幂等正确性的根基。</p>
 *
 * <p>当 Repository 能参与业务事务，且存在 IdempotencyTransactionCoordinator 时，执行被拆成：</p>
 * <pre>
 * Tx-A REQUIRES_NEW : tryAcquire / tryRecover -> PROCESSING -> COMMIT
 * Tx-B REQUIRED     : business callback + markSuccess -> 一起 COMMIT / ROLLBACK
 * Tx-C REQUIRES_NEW : Tx-B 失败以后 markFailed -> 独立 COMMIT
 * </pre>
 */
public final class DefaultIdempotencyExecutor implements IdempotencyExecutor {

    /** 根据 mode/repositoryName 选择真正的状态存储实现。 */
    private final IdempotencyRepositoryRegistry registry;

    /** Starter 组装好的组件默认策略，不让 Core 直接感知 Spring 配置。 */
    private final IdempotencyDefaults defaults;

    /** 为每次新的 PROCESSING generation 创建 ownerToken。 */
    private final IdempotencyOwnerTokenGenerator ownerGenerator;

    /** 把业务异常转换为 failureCode + retryable 等稳定语义。 */
    private final IdempotencyFailureClassifier failureClassifier;

    /** storeResult=true 时用于保存和回放成功结果；允许为空。 */
    private final IdempotencyResultCodec codec;

    /** 可选并发协调层；它只保护状态抢占短临界区，不保护整个 callback。 */
    private final DistributedLockClient lockClient;

    /** 可选事务集成层；只有 Repository 同时声明可参与业务事务时才会启用 Tx-B。 */
    private final IdempotencyTransactionCoordinator transactionCoordinator;

    /** 旁路事件发布器；默认 no-op。 */
    private final IdempotencyEventPublisher events;

    /** 旁路指标记录器；默认 no-op。 */
    private final IdempotencyMetrics metrics;

    /** 统一时间源，方便测试 processing/window/retention 语义。 */
    private final Clock clock;

    /**
     * V1.1 兼容构造：未提供事务协调器时保持原先“业务与 SUCCESS 不保证同事务”的行为。
     */
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
        this(
                registry, defaults, ownerGenerator, failureClassifier, codec, lockClient,
                null, events, metrics, clock);
    }

    /**
     * V1.2 完整构造：transactionCoordinator 非空时，支持 Tx-B 业务事务闭环。
     */
    public DefaultIdempotencyExecutor(
            IdempotencyRepositoryRegistry registry,
            IdempotencyDefaults defaults,
            IdempotencyOwnerTokenGenerator ownerGenerator,
            IdempotencyFailureClassifier failureClassifier,
            IdempotencyResultCodec codec,
            DistributedLockClient lockClient,
            IdempotencyTransactionCoordinator transactionCoordinator,
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
        this.transactionCoordinator = transactionCoordinator;
        this.events = events == null ? IdempotencyEventPublisher.noop() : events;
        this.metrics = metrics == null ? IdempotencyMetrics.noop() : metrics;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    /**
     * 普通业务执行入口。
     *
     * <p>主流程固定为：</p>
     * <ol>
     *     <li>解析 Options 并选择 Repository；</li>
     *     <li>生成 ownerToken；</li>
     *     <li>在可选短分布式锁内调用 Repository.tryAcquire()；</li>
     *     <li>只有 ACQUIRED 才离开锁后执行 callback；</li>
     *     <li>callback 成功后使用 ownerToken + version 条件写 SUCCESS；</li>
     *     <li>callback 失败后写 FAILED。</li>
     * </ol>
     *
     * <p>普通 execute() 遇到 PROCESSING_EXPIRED 或 FAILED_RETRYABLE 只返回状态，
     * 不会自动接管。恢复必须走 {@link #recover}。</p>
     */
    @Override
    public <T> IdempotencyResult<T> execute(
            IdempotencyRequest request,
            Class<T> resultType,
            IdempotencyCallback<T> callback) {

        Objects.requireNonNull(callback, "callback must not be null");
        Instant startedAt = Instant.now(clock);

        // 第 1 步：把外部请求配置解析成一次执行使用的不可变策略快照，并选择 Repository。
        PreparedExecution prepared;
        try {
            prepared = prepare(request == null ? null : request.getOptions(), resultType);
            validateNormalRequest(request);
        } catch (RuntimeException error) {
            return invalid(error);
        }

        IdempotencyOptions options = prepared.options;
        IdempotencyRepository repository = prepared.repository;
        // 第 2 步：每一次“尝试开启新 generation”都使用新的 ownerToken。
        String ownerToken = ownerGenerator.generate(options.getNamespace(), request.getKey());

        // 第 3 步：构造 Repository 层原子抢占请求。
        // processingTimeout/window/retention 都在 Repository 内落成真正的状态语义。
        IdempotencyAcquireRequest acquireRequest = new IdempotencyAcquireRequest(
                options.getNamespace(),
                request.getKey(),
                normalize(request.getRequestHash()),
                normalize(request.getRouteKey()),
                ownerToken,
                options.getMode(),
                options.getProcessingTimeout(),
                options.getIdempotencyWindow(),
                options.getWindowPolicy(),
                options.getRecordRetentionTtl(),
                options.getRecoveryMode(),
                Instant.now(clock));

        publish(IdempotencyEventType.ACQUIRE_ATTEMPT, IdempotencyStage.ACQUIRE_STATE,
                options, repository, null);

        // 第 4 步：可选 DistributedLockClient 只包住 tryAcquire 这一小段。
        // callback 不在锁内，因此不会把 5~30 秒的业务耗时变成长时间 Redis 锁。
        StateInvocation<IdempotencyAcquireResult> invocation = invokeWithOptionalLock(
                options,
                repository,
                request.getRouteKey(),
                request.getKey(),
                () -> repository.tryAcquire(acquireRequest));

        if (invocation.lockRejected) {
            return lockRejected(invocation.error);
        }

        IdempotencyAcquireResult acquire = invocation.result;
        metrics.recordAcquire(options.getMode(), repository.providerName(), acquire.getStatus().name());

        // 第 5 步：Repository 已经把持久状态翻译成明确的“决策状态”。
        // 只有 ACQUIRED 会进入真实业务；其他分支都不会重复执行 callback。
        return switch (acquire.getStatus()) {
            case ACQUIRED -> executeOwned(
                    request.getKey(), request.getRouteKey(), options, repository,
                    acquire.getRecord(), callback, startedAt, invocation.lockFallback, false);
            case SUCCESS -> replay(acquire.getRecord(), resultType, invocation.lockFallback);
            case PROCESSING_ACTIVE -> simple(
                    IdempotencyResultStatus.PROCESSING,
                    IdempotencyStage.ACQUIRE_STATE,
                    acquire.getRecord(), null, invocation.lockFallback);
            case PROCESSING_EXPIRED -> simple(
                    IdempotencyResultStatus.PROCESSING_EXPIRED,
                    IdempotencyStage.ACQUIRE_STATE,
                    acquire.getRecord(), null, invocation.lockFallback);
            case FAILED_RETRYABLE -> simple(
                    IdempotencyResultStatus.PREVIOUS_FAILED_RETRYABLE,
                    IdempotencyStage.ACQUIRE_STATE,
                    acquire.getRecord(), null, invocation.lockFallback);
            case FAILED_FINAL -> simple(
                    IdempotencyResultStatus.PREVIOUS_FAILED_FINAL,
                    IdempotencyStage.ACQUIRE_STATE,
                    acquire.getRecord(), null, invocation.lockFallback);
            case KEY_CONFLICT -> simple(
                    IdempotencyResultStatus.KEY_CONFLICT,
                    IdempotencyStage.ACQUIRE_STATE,
                    acquire.getRecord(), null, invocation.lockFallback);
            case PROVIDER_ERROR -> simple(
                    IdempotencyResultStatus.REPOSITORY_ERROR,
                    IdempotencyStage.ACQUIRE_STATE,
                    acquire.getRecord(), acquire.getError(), invocation.lockFallback);
        };
    }

    /**
     * Reliable Task 的显式恢复入口。
     *
     * <p>recover() 与 execute() 的最大区别是：它允许在满足 expectedOwner/version、
     * processingTimeout、failureRetryable 等条件后开启新的 generation。</p>
     *
     * <p>扫描、分页、调度、MQ 投递都不在这里实现。外部任务组件只负责发现候选并调用本方法，
     * 真正的原子接管仍由 IdempotencyRepository.tryRecover() 完成。</p>
     */
    @Override
    public <T> IdempotencyResult<T> recover(
            IdempotencyRecoveryRequest request,
            Class<T> resultType,
            IdempotencyCallback<T> callback) {

        Objects.requireNonNull(callback, "callback must not be null");
        Instant startedAt = Instant.now(clock);

        PreparedExecution prepared;
        try {
            prepared = prepare(request == null ? null : request.getOptions(), resultType);
            validateRecoveryRequest(request);
        } catch (RuntimeException error) {
            return invalid(error);
        }

        IdempotencyOptions options = prepared.options;
        IdempotencyRepository repository = prepared.repository;

        // 只有显式声明 EXTERNAL_TASK 的记录才允许进入可靠恢复链路。
        if (options.getRecoveryMode() != IdempotencyRecoveryMode.EXTERNAL_TASK) {
            return simple(
                    IdempotencyResultStatus.RECOVERY_NOT_ALLOWED,
                    IdempotencyStage.RECOVER_STATE,
                    null,
                    new IllegalStateException("recoveryMode must be EXTERNAL_TASK"),
                    false);
        }

        // 新一代恢复执行者必须拥有新的 ownerToken；Repository 还会把 version + 1。
        String newOwner = ownerGenerator.generate(options.getNamespace(), request.getKey());
        IdempotencyRecoveryAcquireRequest recoveryRequest = new IdempotencyRecoveryAcquireRequest(
                options.getNamespace(),
                request.getKey(),
                normalize(request.getRequestHash()),
                normalize(request.getRouteKey()),
                newOwner,
                normalize(request.getExpectedOwnerToken()),
                request.getExpectedVersion(),
                options.getMode(),
                options.getProcessingTimeout(),
                options.isRecoverFailed(),
                Instant.now(clock));

        publish(IdempotencyEventType.RECOVERY_ATTEMPT, IdempotencyStage.RECOVER_STATE,
                options, repository, null);

        // 与普通抢占一样，分布式锁仍然只保护“状态接管”短临界区。
        StateInvocation<IdempotencyRecoveryResult> invocation = invokeWithOptionalLock(
                options,
                repository,
                request.getRouteKey(),
                request.getKey(),
                () -> repository.tryRecover(recoveryRequest));

        if (invocation.lockRejected) {
            return lockRejected(invocation.error);
        }

        IdempotencyRecoveryResult recovery = invocation.result;
        return switch (recovery.getStatus()) {
            case RECOVERY_ACQUIRED -> executeOwned(
                    request.getKey(), request.getRouteKey(), options, repository,
                    recovery.getRecord(), callback, startedAt, invocation.lockFallback, true);
            case SUCCESS -> replay(recovery.getRecord(), resultType, invocation.lockFallback);
            case PROCESSING_ACTIVE -> simple(
                    IdempotencyResultStatus.PROCESSING,
                    IdempotencyStage.RECOVER_STATE,
                    recovery.getRecord(), null, invocation.lockFallback);
            case NOT_RECOVERABLE, FAILED_FINAL, NOT_FOUND -> simple(
                    IdempotencyResultStatus.RECOVERY_NOT_ALLOWED,
                    IdempotencyStage.RECOVER_STATE,
                    recovery.getRecord(), null, invocation.lockFallback);
            case STALE_CANDIDATE -> simple(
                    IdempotencyResultStatus.STALE_RECOVERY_CANDIDATE,
                    IdempotencyStage.RECOVER_STATE,
                    recovery.getRecord(), null, invocation.lockFallback);
            case KEY_CONFLICT -> simple(
                    IdempotencyResultStatus.KEY_CONFLICT,
                    IdempotencyStage.RECOVER_STATE,
                    recovery.getRecord(), null, invocation.lockFallback);
            case PROVIDER_ERROR -> simple(
                    IdempotencyResultStatus.REPOSITORY_ERROR,
                    IdempotencyStage.RECOVER_STATE,
                    recovery.getRecord(), recovery.getError(), invocation.lockFallback);
        };
    }

    /**
     * 统一完成 Options 默认值、参数校验和 Repository 选择。
     *
     * <p>storeResult=true 时必须同时具备 codec + resultType，否则第一次虽然能保存，
     * 后续 replay 却无法可靠反序列化，所以在业务执行前直接 fail fast。</p>
     */
    private PreparedExecution prepare(IdempotencyOptions requestOptions, Class<?> resultType) {
        IdempotencyOptions options = requestOptions == null ? defaults.defaultOptions() : requestOptions;
        options.validate();
        if (options.isStoreResult() && codec == null) {
            throw new IllegalArgumentException("storeResult=true requires IdempotencyResultCodec");
        }
        if (options.isStoreResult() && resultType == null) {
            throw new IllegalArgumentException(
                    "storeResult=true requires resultType so replay can deserialize stored result");
        }
        return new PreparedExecution(
                options,
                registry.resolve(options.getMode(), options.getRepositoryName()));
    }

    private void validateNormalRequest(IdempotencyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        validateKey(request.getKey());
    }

    private void validateRecoveryRequest(IdempotencyRecoveryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("recovery request must not be null");
        }
        validateKey(request.getKey());
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("idempotency key must not be blank");
        }
    }

    /**
     * 已经拿到 PROCESSING 执行权后的真实业务执行阶段。
     *
     * <p>进入本方法时，外层短分布式锁已经释放。此后是否仍有执行权，
     * 最终由 markSuccess/markFailed 的 ownerToken + version CAS 再次确认。</p>
     *
     * <p>V1.2 如果 Repository 与 transaction-component 都具备事务参与能力，会自动进入 Tx-B：
     * callback 与 markSuccess 在同一个 REQUIRED 本地事务中完成。否则保持 V1.1 非事务闭环。</p>
     */
    private <T> IdempotencyResult<T> executeOwned(
            String key,
            String routeKey,
            IdempotencyOptions options,
            IdempotencyRepository repository,
            IdempotencyRecord record,
            IdempotencyCallback<T> callback,
            Instant startedAt,
            boolean lockFallback,
            boolean recoveryExecution) {

        publish(IdempotencyEventType.EXECUTION_STARTED, IdempotencyStage.EXECUTE,
                options, repository, null);

        // 把当前 generation 的 owner/version 暴露给业务。
        // 高风险业务可以继续使用 context.fencingVersion() 做业务资源条件更新。
        IdempotencyContext context = new IdempotencyContext(
                options.getNamespace(),
                key,
                normalize(routeKey),
                record.getOwnerToken(),
                record.getVersion(),
                options.getMode(),
                recoveryExecution,
                record.getUpdatedAt() == null ? Instant.now(clock) : record.getUpdatedAt(),
                record.getProcessingExpireAt());

        boolean transactionApplied = transactionCoordinator != null
                && repository.supportsBusinessTransactionParticipation();

        if (transactionApplied) {
            return executeOwnedTransactionally(
                    key, routeKey, options, repository, record, callback, context,
                    startedAt, lockFallback, recoveryExecution);
        }

        return executeOwnedWithoutBusinessTransaction(
                key, options, repository, record, callback, context,
                startedAt, lockFallback, recoveryExecution);
    }

    /**
     * V1.2 Tx-B：业务 callback 与 markSuccess 必须处于同一 REQUIRED 本地事务。
     *
     * <p>这里有一个非常关键的规则：markSuccess 只要不是 UPDATED，就必须抛出内部异常让 Tx-B 回滚。
     * 不能“业务 SQL 已提交，但幂等 SUCCESS 写失败后只返回一个错误结果”，否则会重新出现业务已成功、
     * 幂等状态仍 PROCESSING 的不一致窗口。</p>
     */
    private <T> IdempotencyResult<T> executeOwnedTransactionally(
            String key,
            String routeKey,
            IdempotencyOptions options,
            IdempotencyRepository repository,
            IdempotencyRecord record,
            IdempotencyCallback<T> callback,
            IdempotencyContext context,
            Instant startedAt,
            boolean lockFallback,
            boolean recoveryExecution) {

        try {
            TransactionalCompletion<T> completion = transactionCoordinator.executeRequired(
                    transactionName(options),
                    normalize(routeKey),
                    () -> {
                        // DistributedLock 已经释放；这里开始真正 Tx-B。
                        T value = callback.doWithIdempotency(context);
                        String resultPayload = encodeResultIfNeeded(value, options);
                        IdempotencyWriteResult write = repository.markSuccess(
                                successRequest(key, options, record, resultPayload, Instant.now(clock)));

                        if (write.getStatus() != IdempotencyWriteStatus.UPDATED) {
                            // 通过异常把 markSuccess 失败转换成整个 Tx-B rollback。
                            throw new CompletionRejectedException(write);
                        }
                        return new TransactionalCompletion<>(value, write.getRecord());
                    });

            publish(IdempotencyEventType.EXECUTION_SUCCESS, IdempotencyStage.COMPLETE_STATE,
                    options, repository, null);
            return finish(
                    recoveryExecution ? IdempotencyResultStatus.RECOVERED
                            : IdempotencyResultStatus.EXECUTED,
                    IdempotencyStage.COMPLETE_STATE,
                    completion.value,
                    completion.record,
                    null,
                    options,
                    repository,
                    startedAt,
                    lockFallback,
                    true);

        } catch (CompletionRejectedException rejected) {
            IdempotencyWriteResult write = rejected.write;
            if (write.getStatus() == IdempotencyWriteStatus.STALE_OWNER
                    || write.getStatus() == IdempotencyWriteStatus.ALREADY_FINAL) {
                publish(IdempotencyEventType.OWNERSHIP_LOST, IdempotencyStage.COMPLETE_STATE,
                        options, repository, null);
                return finish(
                        IdempotencyResultStatus.OWNERSHIP_LOST,
                        IdempotencyStage.COMPLETE_STATE,
                        null,
                        write.getRecord(),
                        write.getError(),
                        options,
                        repository,
                        startedAt,
                        lockFallback,
                        true);
            }

            publish(IdempotencyEventType.REPOSITORY_ERROR, IdempotencyStage.COMPLETE_STATE,
                    options, repository, write.getError());
            return finish(
                    IdempotencyResultStatus.REPOSITORY_ERROR,
                    IdempotencyStage.COMPLETE_STATE,
                    null,
                    write.getRecord(),
                    write.getError(),
                    options,
                    repository,
                    startedAt,
                    lockFallback,
                    true);

        } catch (ResultEncodeException codecError) {
            // Tx-B 已因异常回滚，可以安全地在 Tx-C 记录一个不可重试技术失败。
            IdempotencyWriteResult failureWrite = persistFailure(
                    key,
                    options,
                    repository,
                    record,
                    new IdempotencyFailureInfo(
                            "RESULT_CODEC_ERROR",
                            safeMessage(codecError.getCause()),
                            false,
                            Instant.now(clock)));
            attachProviderFailure(codecError, failureWrite);

            return finish(
                    IdempotencyResultStatus.RESULT_CODEC_ERROR,
                    IdempotencyStage.COMPLETE_STATE,
                    null,
                    failureWrite.getRecord() == null ? record : failureWrite.getRecord(),
                    codecError.getCause(),
                    options,
                    repository,
                    startedAt,
                    lockFallback,
                    true);

        } catch (IdempotencyTransactionException transactionError) {
            return handleTransactionFailure(
                    key,
                    options,
                    repository,
                    record,
                    transactionError,
                    startedAt,
                    lockFallback);

        } catch (Throwable businessError) {
            // TransactionCoordinator 已经先让 Tx-B 回滚；这里再进入 Tx-C 记录 FAILED。
            return handleBusinessFailure(
                    key,
                    options,
                    repository,
                    record,
                    businessError,
                    startedAt,
                    lockFallback,
                    true);
        }
    }

    /**
     * 未接入 transaction-component 时保留的 V1.1 执行路径。
     *
     * <p>该路径仍然依靠 ownerToken + version CAS 保证旧 owner 不能覆盖新 generation，
     * 但不能承诺“业务写 + SUCCESS”是同一个本地事务。</p>
     */
    private <T> IdempotencyResult<T> executeOwnedWithoutBusinessTransaction(
            String key,
            IdempotencyOptions options,
            IdempotencyRepository repository,
            IdempotencyRecord record,
            IdempotencyCallback<T> callback,
            IdempotencyContext context,
            Instant startedAt,
            boolean lockFallback,
            boolean recoveryExecution) {
        try {
            T value = callback.doWithIdempotency(context);
            String resultPayload = encodeResultIfNeeded(value, options);
            IdempotencyWriteResult write = repository.markSuccess(
                    successRequest(key, options, record, resultPayload, Instant.now(clock)));

            if (write.getStatus() == IdempotencyWriteStatus.UPDATED) {
                publish(IdempotencyEventType.EXECUTION_SUCCESS, IdempotencyStage.COMPLETE_STATE,
                        options, repository, null);
                return finish(
                        recoveryExecution ? IdempotencyResultStatus.RECOVERED
                                : IdempotencyResultStatus.EXECUTED,
                        IdempotencyStage.COMPLETE_STATE,
                        value,
                        write.getRecord(),
                        null,
                        options,
                        repository,
                        startedAt,
                        lockFallback,
                        false);
            }

            if (write.getStatus() == IdempotencyWriteStatus.STALE_OWNER
                    || write.getStatus() == IdempotencyWriteStatus.ALREADY_FINAL) {
                publish(IdempotencyEventType.OWNERSHIP_LOST, IdempotencyStage.COMPLETE_STATE,
                        options, repository, null);
                return finish(
                        IdempotencyResultStatus.OWNERSHIP_LOST,
                        IdempotencyStage.COMPLETE_STATE,
                        null,
                        write.getRecord(),
                        null,
                        options,
                        repository,
                        startedAt,
                        lockFallback,
                        false);
            }

            return finish(
                    IdempotencyResultStatus.REPOSITORY_ERROR,
                    IdempotencyStage.COMPLETE_STATE,
                    null,
                    write.getRecord(),
                    write.getError(),
                    options,
                    repository,
                    startedAt,
                    lockFallback,
                    false);

        } catch (ResultEncodeException error) {
            return finish(
                    IdempotencyResultStatus.RESULT_CODEC_ERROR,
                    IdempotencyStage.COMPLETE_STATE,
                    null,
                    record,
                    error.getCause(),
                    options,
                    repository,
                    startedAt,
                    lockFallback,
                    false);
        } catch (Throwable businessError) {
            return handleBusinessFailure(
                    key,
                    options,
                    repository,
                    record,
                    businessError,
                    startedAt,
                    lockFallback,
                    false);
        }
    }

    private IdempotencySuccessRequest successRequest(
            String key,
            IdempotencyOptions options,
            IdempotencyRecord record,
            String resultPayload,
            Instant now) {
        return new IdempotencySuccessRequest(
                options.getNamespace(),
                key,
                record.getOwnerToken(),
                record.getVersion(),
                resultPayload,
                options.getMode(),
                options.getIdempotencyWindow(),
                options.getWindowPolicy(),
                options.getRecordRetentionTtl(),
                now);
    }

    /**
     * 根据 storeResult 配置生成可持久化结果快照。
     */
    private String encodeResultIfNeeded(Object value, IdempotencyOptions options)
            throws ResultEncodeException {
        if (!options.isStoreResult()) {
            return null;
        }
        try {
            return codec.encode(value);
        } catch (Exception error) {
            throw new ResultEncodeException(error);
        }
    }

    /**
     * callback 抛异常后的失败收敛逻辑。
     *
     * <p>事务集成启用时，进入这里之前 Tx-B 已经回滚；JdbcIdempotencyRepository.markFailed()
     * 再通过 Tx-C = REQUIRES_NEW 独立提交 FAILED。</p>
     */
    private <T> IdempotencyResult<T> handleBusinessFailure(
            String key,
            IdempotencyOptions options,
            IdempotencyRepository repository,
            IdempotencyRecord record,
            Throwable businessError,
            Instant startedAt,
            boolean lockFallback,
            boolean transactionApplied) {

        if (businessError instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }

        Instant failedAt = Instant.now(clock);
        IdempotencyFailureInfo failure = failureClassifier.classify(businessError, failedAt);
        IdempotencyWriteResult write = persistFailure(
                key, options, repository, record, failure);
        attachProviderFailure(businessError, write);

        publish(IdempotencyEventType.EXECUTION_FAILED, IdempotencyStage.EXECUTE,
                options, repository, businessError);

        return finish(
                IdempotencyResultStatus.EXECUTION_FAILED,
                IdempotencyStage.EXECUTE,
                null,
                write.getRecord() == null ? record : write.getRecord(),
                businessError,
                options,
                repository,
                startedAt,
                lockFallback,
                transactionApplied);
    }

    /**
     * Tx-B 自身的基础设施失败处理。
     *
     * <p>COMMIT_UNKNOWN 是最危险的情况：业务与 SUCCESS 可能已经一起提交，也可能一起回滚。
     * 因此这里绝不能抢先把记录改成 FAILED，而是保留 PROCESSING，等待下一次查询或超时恢复来定夺。</p>
     */
    private <T> IdempotencyResult<T> handleTransactionFailure(
            String key,
            IdempotencyOptions options,
            IdempotencyRepository repository,
            IdempotencyRecord record,
            IdempotencyTransactionException transactionError,
            Instant startedAt,
            boolean lockFallback) {

        if (transactionError.outcome() == IdempotencyTransactionOutcome.COMMIT_UNKNOWN) {
            publish(IdempotencyEventType.TRANSACTION_COMMIT_UNKNOWN, IdempotencyStage.TRANSACTION,
                    options, repository, transactionError);
            return finish(
                    IdempotencyResultStatus.TRANSACTION_COMMIT_UNKNOWN,
                    IdempotencyStage.TRANSACTION,
                    null,
                    record,
                    transactionError,
                    options,
                    repository,
                    startedAt,
                    lockFallback,
                    true);
        }

        // BEGIN 失败或明确 rollback 时，Tx-B 没有成功提交，可以进入 Tx-C 记录可恢复失败。
        IdempotencyWriteResult write = persistFailure(
                key,
                options,
                repository,
                record,
                new IdempotencyFailureInfo(
                        "TRANSACTION_" + transactionError.outcome().name(),
                        safeMessage(transactionError),
                        true,
                        Instant.now(clock)));
        attachProviderFailure(transactionError, write);

        publish(IdempotencyEventType.TRANSACTION_FAILED, IdempotencyStage.TRANSACTION,
                options, repository, transactionError);
        return finish(
                IdempotencyResultStatus.TRANSACTION_FAILED,
                IdempotencyStage.TRANSACTION,
                null,
                write.getRecord() == null ? record : write.getRecord(),
                transactionError,
                options,
                repository,
                startedAt,
                lockFallback,
                true);
    }

    /**
     * 统一构造 PROCESSING -> FAILED 请求。
     * Jdbc Provider V1.2 会把这一步放进独立 Tx-C；Redis Provider 继续使用自己的原子写语义。
     */
    private IdempotencyWriteResult persistFailure(
            String key,
            IdempotencyOptions options,
            IdempotencyRepository repository,
            IdempotencyRecord record,
            IdempotencyFailureInfo failure) {
        return repository.markFailed(
                new IdempotencyFailureRequest(
                        options.getNamespace(),
                        key,
                        record.getOwnerToken(),
                        record.getVersion(),
                        failure,
                        options.getMode(),
                        options.getIdempotencyWindow(),
                        options.getWindowPolicy(),
                        options.getRecordRetentionTtl(),
                        failure.getOccurredAt()));
    }

    private void attachProviderFailure(Throwable primary, IdempotencyWriteResult write) {
        if (write != null
                && write.getStatus() == IdempotencyWriteStatus.PROVIDER_ERROR
                && write.getError() != null
                && write.getError() != primary) {
            primary.addSuppressed(write.getError());
        }
    }

    private String transactionName(IdempotencyOptions options) {
        // TransactionEvent 可能被打日志/指标，因此不要把 key / routeKey 这类高基数值放进事务名。
        return "idempotency-business:" + options.getNamespace();
    }

    private String safeMessage(Throwable error) {
        if (error == null) {
            return null;
        }
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }

    /**
     * 可选分布式锁只减少同 key 同时打到 Repository 的竞争。
     *
     * <p>锁名包含 routeKey，避免分片场景下不同路由域错误共享同一个协调键。</p>
     */
    private <R> StateInvocation<R> invokeWithOptionalLock(
            IdempotencyOptions options,
            IdempotencyRepository repository,
            String routeKey,
            String key,
            StateOperation<R> operation) {

        IdempotencyLockOptions lock = options.getLockOptions();
        if (!lock.isEnabled()) {
            return StateInvocation.direct(operation.invoke());
        }

        if (lockClient == null) {
            if (lock.isFallbackToStateOnFailure()) {
                publish(IdempotencyEventType.LOCK_FALLBACK, IdempotencyStage.LOCK,
                        options, repository, null);
                return StateInvocation.fallback(operation.invoke());
            }
            return StateInvocation.lockRejected(
                    new IllegalStateException("lock enabled but DistributedLockClient unavailable"));
        }

        LockWaitStrategy waitStrategy = lock.getWaitTime().isZero()
                ? LockWaitStrategy.NO_WAIT : LockWaitStrategy.BACKOFF;

        LockOptions lockOptions = LockOptions.builder()
                .namespace("idempotency:" + options.getNamespace())
                .providerName(lock.getProviderName())
                .waitTime(lock.getWaitTime())
                .waitStrategy(waitStrategy)
                .leaseTime(lock.getLeaseTime())
                .autoRenew(false)
                .fencingRequired(false)
                .build();

        String lockName = "state:" + routePart(routeKey) + ":" + key;
        LockResult<R> result = lockClient.execute(
                lockName,
                lockOptions,
                (LockCallback<R>) handle -> operation.invoke());

        if (result.isSuccess() && result.value().isPresent()) {
            return StateInvocation.direct(result.value().get());
        }

        if (lock.isFallbackToStateOnFailure()) {
            publish(IdempotencyEventType.LOCK_FALLBACK, IdempotencyStage.LOCK,
                    options, repository, result.error().orElse(null));
            return StateInvocation.fallback(operation.invoke());
        }

        return StateInvocation.lockRejected(
                result.error().orElseGet(() -> new IllegalStateException(
                        "distributed lock not acquired: " + result.status())));
    }

    private String routePart(String routeKey) {
        String normalized = normalize(routeKey);
        return normalized == null ? "_" : normalized;
    }

    /**
     * SUCCESS 重复请求的结果回放。
     *
     * <p>如果没有保存 resultPayload，也仍然返回 REPLAYED，表示“历史上已经成功”；
     * 如果保存了 payload，则需要 codec + resultType 才能恢复成 T。</p>
     */
    private <T> IdempotencyResult<T> replay(
            IdempotencyRecord record,
            Class<T> resultType,
            boolean lockFallback) {

        if (record == null || record.getResultPayload() == null || record.getResultPayload().isBlank()) {
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
                    .error(new IllegalStateException("stored result exists but resultType/codec missing"))
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

    private <T> IdempotencyResult<T> invalid(Throwable error) {
        return IdempotencyResult.<T>builder()
                .status(IdempotencyResultStatus.INVALID_OPTIONS)
                .stage(IdempotencyStage.VALIDATE)
                .error(error)
                .build();
    }

    private <T> IdempotencyResult<T> lockRejected(Throwable error) {
        return IdempotencyResult.<T>builder()
                .status(IdempotencyResultStatus.LOCK_NOT_ACQUIRED)
                .stage(IdempotencyStage.LOCK)
                .error(error)
                .build();
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
            boolean lockFallback,
            boolean transactionApplied) {

        metrics.recordExecution(
                options.getMode(), repository.providerName(), status,
                Duration.between(startedAt, Instant.now(clock)));

        return IdempotencyResult.<T>builder()
                .status(status)
                .stage(stage)
                .value(value)
                .record(record)
                .error(error)
                .lockFallback(lockFallback)
                .transactionApplied(transactionApplied)
                .build();
    }

    private void publish(
            IdempotencyEventType type,
            IdempotencyStage stage,
            IdempotencyOptions options,
            IdempotencyRepository repository,
            Throwable error) {
        events.publish(new IdempotencyEvent(
                type, stage, options.getMode(), repository.providerName(),
                Instant.now(clock), error));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /** Repository 状态操作的延迟执行封装，用于决定是否套可选 DistributedLock。 */
    @FunctionalInterface
    private interface StateOperation<R> {
        R invoke();
    }

    /** 一次调用完成 prepare() 后的内部快照，避免后续重复解析 Options/Repository。 */
    private static final class PreparedExecution {
        private final IdempotencyOptions options;
        private final IdempotencyRepository repository;

        private PreparedExecution(IdempotencyOptions options, IdempotencyRepository repository) {
            this.options = options;
            this.repository = repository;
        }
    }

    /**
     * 可选分布式锁执行结果。
     *
     * <p>lockFallback=true 表示锁失败后按配置直接退化到 Repository 原子状态机；
     * lockRejected=true 表示配置要求锁失败即终止。</p>
     */
    private static final class StateInvocation<R> {
        private final R result;
        private final boolean lockFallback;
        private final boolean lockRejected;
        private final Throwable error;

        private StateInvocation(R result, boolean lockFallback, boolean lockRejected, Throwable error) {
            this.result = result;
            this.lockFallback = lockFallback;
            this.lockRejected = lockRejected;
            this.error = error;
        }

        private static <R> StateInvocation<R> direct(R result) {
            return new StateInvocation<>(result, false, false, null);
        }

        private static <R> StateInvocation<R> fallback(R result) {
            return new StateInvocation<>(result, true, false, null);
        }

        private static <R> StateInvocation<R> lockRejected(Throwable error) {
            return new StateInvocation<>(null, false, true, error);
        }
    }

    /** Tx-B 已完成 callback，但最终幂等状态不能提交时，用异常强制事务整体回滚。 */
    private static final class CompletionRejectedException extends RuntimeException {
        private final IdempotencyWriteResult write;

        private CompletionRejectedException(IdempotencyWriteResult write) {
            super("idempotency completion rejected: "
                    + (write == null ? "null" : write.getStatus()));
            this.write = Objects.requireNonNull(write, "write must not be null");
        }
    }

    /** Tx-B 正常提交前暂存业务值和 SUCCESS 状态快照。 */
    private static final class TransactionalCompletion<T> {
        private final T value;
        private final IdempotencyRecord record;

        private TransactionalCompletion(T value, IdempotencyRecord record) {
            this.value = value;
            this.record = record;
        }
    }

    private static final class ResultEncodeException extends Exception {
        private ResultEncodeException(Throwable cause) {
            super(cause);
        }
    }
}
