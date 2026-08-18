package com.xjtu.iron.idempotent.core.execution;

import com.xjtu.iron.idempotent.core.repository.IdempotencyRepositoryRegistry;
import com.xjtu.iron.idempotent.core.policy.IdempotencyPolicyRegistry;
import com.xjtu.iron.idempotent.core.owner.IdempotencyOwnerTokenGenerator;

import com.xjtu.iron.distributed.lock.api.DistributedLockClient;
import com.xjtu.iron.distributed.lock.api.LockCallback;
import com.xjtu.iron.distributed.lock.api.LockOptions;
import com.xjtu.iron.distributed.lock.api.LockResult;
import com.xjtu.iron.distributed.lock.api.LockWaitStrategy;
import com.xjtu.iron.idempotent.api.execution.*;
import com.xjtu.iron.idempotent.api.policy.*;
import com.xjtu.iron.idempotent.api.recovery.*;
import com.xjtu.iron.idempotent.api.state.*;
import com.xjtu.iron.idempotent.api.repository.*;
import com.xjtu.iron.idempotent.api.repository.acquire.*;
import com.xjtu.iron.idempotent.api.repository.recovery.*;
import com.xjtu.iron.idempotent.api.repository.write.*;
import com.xjtu.iron.idempotent.api.result.IdempotencyResultPolicies;
import com.xjtu.iron.idempotent.api.result.IdempotencyResultPolicy;
import com.xjtu.iron.idempotent.api.result.IdempotencyResultPolicyType;
import com.xjtu.iron.idempotent.api.spi.IdempotencyFailureClassifier;
import com.xjtu.iron.idempotent.core.observation.*;
import com.xjtu.iron.idempotent.core.result.StoredResultEnvelope;
import com.xjtu.iron.idempotent.core.state.*;
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
 * <p>核心链路：</p>
 * <pre>
 * Request
 *   -> Policy Resolution
 *   -> Repository capabilities
 *   -> optional DistributedLock (只包状态抢占)
 *   -> Repository CAS/Lua
 *   -> State Machine decision
 *   -> Business
 *   -> optional Tx-B
 *   -> final state CAS
 * </pre>
 *
 * <p>WINDOWED 与 DURABLE 共用同一套状态机；差异由 Policy 与 Repository capabilities 表达。</p>
 */
public final class DefaultIdempotencyExecutor implements IdempotencyExecutor {

    private final IdempotencyRepositoryRegistry repositoryRegistry;
    private final IdempotencyPolicyRegistry policyRegistry;
    private final IdempotencyOwnerTokenGenerator ownerGenerator;
    private final IdempotencyFailureClassifier failureClassifier;
    private final DistributedLockClient lockClient;
    private final IdempotencyTransactionCoordinator transactionCoordinator;
    private final IdempotencyStateMachine stateMachine;
    private final IdempotencyEventPublisher events;
    private final IdempotencyMetrics metrics;
    private final Clock clock;

    public DefaultIdempotencyExecutor(
            IdempotencyRepositoryRegistry repositoryRegistry,
            IdempotencyPolicyRegistry policyRegistry,
            IdempotencyOwnerTokenGenerator ownerGenerator,
            IdempotencyFailureClassifier failureClassifier,
            DistributedLockClient lockClient,
            IdempotencyTransactionCoordinator transactionCoordinator,
            IdempotencyStateMachine stateMachine,
            IdempotencyEventPublisher events,
            IdempotencyMetrics metrics,
            Clock clock) {
        this.repositoryRegistry = Objects.requireNonNull(
                repositoryRegistry, "repositoryRegistry must not be null");
        this.policyRegistry = Objects.requireNonNull(
                policyRegistry, "policyRegistry must not be null");
        this.ownerGenerator = Objects.requireNonNull(
                ownerGenerator, "ownerGenerator must not be null");
        this.failureClassifier = Objects.requireNonNull(
                failureClassifier, "failureClassifier must not be null");
        this.lockClient = lockClient;
        this.transactionCoordinator = transactionCoordinator;
        this.stateMachine = stateMachine == null
                ? new DefaultIdempotencyStateMachine() : stateMachine;
        this.events = events == null ? IdempotencyEventPublisher.noop() : events;
        this.metrics = metrics == null ? IdempotencyMetrics.noop() : metrics;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    public <T> IdempotencyResult<T> execute(
            IdempotencyRequest request,
            IdempotencyResultPolicy<T> resultPolicy,
            IdempotencyCallback<T> callback) {

        Objects.requireNonNull(callback, "callback must not be null");
        Instant startedAt = Instant.now(clock);

        IdempotencyExecutionDefinition<T> definition;
        try {
            validateNormalRequest(request);
            definition = prepare(
                    request.getPolicyName(),
                    request.getPolicy(),
                    resultPolicy);
        } catch (RuntimeException error) {
            return invalid(error);
        }

        IdempotencyPolicy policy = definition.policy();
        IdempotencyRepository repository = definition.repository();
        String ownerToken = ownerGenerator.generate(policy.getNamespace(), request.getKey());

        IdempotencyAcquireRequest acquireRequest = new IdempotencyAcquireRequest(
                policy.getNamespace(),
                request.getKey(),
                normalize(request.getRequestHash()),
                normalize(request.getRouteKey()),
                ownerToken,
                policy.getMode(),
                policy.getProcessingTimeout(),
                policy.getIdempotencyWindow(),
                policy.getWindowPolicy(),
                policy.getRecordRetentionTtl(),
                policy.getRecoveryPolicy().getMode(),
                Instant.now(clock));

        publish(IdempotencyEventType.ACQUIRE_ATTEMPT, IdempotencyStage.ACQUIRE_STATE,
                policy, repository, null);

        StateInvocation<IdempotencyAcquireResult> invocation = invokeWithOptionalLock(
                policy,
                repository,
                request.getRouteKey(),
                request.getKey(),
                () -> repository.tryAcquire(acquireRequest));

        if (invocation.lockRejected) {
            return lockRejected(invocation.error);
        }

        IdempotencyAcquireResult acquire = invocation.result;
        metrics.recordAcquire(policy.getMode(), repository.providerName(), acquire.getStatus().name());

        IdempotencyStateDecision decision = stateMachine.onAcquire(acquire.getStatus());
        return applyAcquireDecision(
                decision,
                request,
                definition,
                acquire,
                callback,
                startedAt,
                invocation.lockFallback);
    }

    @Override
    public <T> IdempotencyResult<T> recover(
            IdempotencyRecoveryRequest request,
            IdempotencyResultPolicy<T> resultPolicy,
            IdempotencyCallback<T> callback) {

        Objects.requireNonNull(callback, "callback must not be null");
        Instant startedAt = Instant.now(clock);

        IdempotencyExecutionDefinition<T> definition;
        try {
            validateRecoveryRequest(request);
            definition = prepare(
                    request.getPolicyName(),
                    request.getPolicy(),
                    resultPolicy);
        } catch (RuntimeException error) {
            return invalid(error);
        }

        IdempotencyPolicy policy = definition.policy();
        IdempotencyRepository repository = definition.repository();
        IdempotencyRecoveryPolicy recoveryPolicy = policy.getRecoveryPolicy();

        if (!recoveryPolicy.isExternalTaskEnabled()) {
            return simple(
                    IdempotencyResultStatus.RECOVERY_NOT_ALLOWED,
                    IdempotencyStage.RECOVER_STATE,
                    null,
                    new IllegalStateException("recovery policy does not enable EXTERNAL_TASK"),
                    false);
        }

        String newOwner = ownerGenerator.generate(policy.getNamespace(), request.getKey());
        IdempotencyRecoveryAcquireRequest recoveryRequest =
                new IdempotencyRecoveryAcquireRequest(
                        policy.getNamespace(),
                        request.getKey(),
                        normalize(request.getRequestHash()),
                        normalize(request.getRouteKey()),
                        newOwner,
                        normalize(request.getExpectedOwnerToken()),
                        request.getExpectedVersion(),
                        policy.getMode(),
                        policy.getProcessingTimeout(),
                        recoveryPolicy.isRecoverProcessingTimeout(),
                        recoveryPolicy.isRecoverRetryableFailure(),
                        Instant.now(clock));

        publish(IdempotencyEventType.RECOVERY_ATTEMPT, IdempotencyStage.RECOVER_STATE,
                policy, repository, null);

        StateInvocation<IdempotencyRecoveryResult> invocation = invokeWithOptionalLock(
                policy,
                repository,
                request.getRouteKey(),
                request.getKey(),
                () -> repository.tryRecover(recoveryRequest));

        if (invocation.lockRejected) {
            return lockRejected(invocation.error);
        }

        IdempotencyRecoveryResult recovery = invocation.result;
        IdempotencyStateDecision decision = stateMachine.onRecovery(recovery.getStatus());

        if (decision.action() == IdempotencyStateAction.EXECUTE) {
            return executeOwned(
                    request.getKey(),
                    request.getRouteKey(),
                    definition,
                    recovery.getRecord(),
                    callback,
                    startedAt,
                    invocation.lockFallback,
                    true);
        }
        if (decision.action() == IdempotencyStateAction.REPLAY) {
            return replay(
                    recovery.getRecord(),
                    definition.resultPolicy(),
                    invocation.lockFallback);
        }

        Throwable error = recovery.getStatus() == IdempotencyRecoveryStatus.PROVIDER_ERROR
                ? recovery.getError() : null;
        return simple(
                decision.resultStatus(),
                IdempotencyStage.RECOVER_STATE,
                recovery.getRecord(),
                error,
                invocation.lockFallback);
    }

    private <T> IdempotencyResult<T> applyAcquireDecision(
            IdempotencyStateDecision decision,
            IdempotencyRequest request,
            IdempotencyExecutionDefinition<T> definition,
            IdempotencyAcquireResult acquire,
            IdempotencyCallback<T> callback,
            Instant startedAt,
            boolean lockFallback) {

        if (decision.action() == IdempotencyStateAction.EXECUTE) {
            return executeOwned(
                    request.getKey(),
                    request.getRouteKey(),
                    definition,
                    acquire.getRecord(),
                    callback,
                    startedAt,
                    lockFallback,
                    false);
        }
        if (decision.action() == IdempotencyStateAction.REPLAY) {
            return replay(acquire.getRecord(), definition.resultPolicy(), lockFallback);
        }

        Throwable error = acquire.getStatus() == IdempotencyAcquireStatus.PROVIDER_ERROR
                ? acquire.getError() : null;
        return simple(
                decision.resultStatus(),
                IdempotencyStage.ACQUIRE_STATE,
                acquire.getRecord(),
                error,
                lockFallback);
    }

    private <T> IdempotencyExecutionDefinition<T> prepare(
            String policyName,
            IdempotencyPolicy inlinePolicy,
            IdempotencyResultPolicy<T> resultPolicy) {

        IdempotencyPolicy policy = policyRegistry.resolve(
                policyName, inlinePolicy);
        policy.validate();

        IdempotencyRepository repository = repositoryRegistry.resolve(
                policy.getMode(), policy.getRepositoryName());

        IdempotencyResultPolicy<T> resolvedResultPolicy = resultPolicy == null
                ? IdempotencyResultPolicies.none()
                : resultPolicy;

        if (resolvedResultPolicy.storesPayload()
                && !repository.capabilities().isResultPayloadSupported()) {
            throw new IllegalArgumentException(
                    "repository " + repository.providerName()
                            + " does not support result payload storage");
        }

        return new IdempotencyExecutionDefinition<>(
                policy, repository, resolvedResultPolicy);
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

    private <T> IdempotencyResult<T> executeOwned(
            String key,
            String routeKey,
            IdempotencyExecutionDefinition<T> definition,
            IdempotencyRecord record,
            IdempotencyCallback<T> callback,
            Instant startedAt,
            boolean lockFallback,
            boolean recoveryExecution) {

        IdempotencyPolicy policy = definition.policy();
        IdempotencyRepository repository = definition.repository();

        publish(IdempotencyEventType.EXECUTION_STARTED, IdempotencyStage.EXECUTE,
                policy, repository, null);

        IdempotencyContext context = new IdempotencyContext(
                policy.getNamespace(),
                key,
                normalize(routeKey),
                record.getOwnerToken(),
                record.getVersion(),
                policy.getMode(),
                recoveryExecution,
                record.getUpdatedAt() == null ? Instant.now(clock) : record.getUpdatedAt(),
                record.getProcessingExpireAt());

        boolean transactionApplied = transactionCoordinator != null
                && repository.capabilities().isBusinessTransactionParticipationSupported();

        if (transactionApplied) {
            return executeOwnedTransactionally(
                    key, routeKey, definition, record, callback, context,
                    startedAt, lockFallback, recoveryExecution);
        }

        return executeOwnedWithoutBusinessTransaction(
                key, definition, record, callback, context,
                startedAt, lockFallback, recoveryExecution);
    }

    /**
     * Tx-B：Business + ResultPolicy.capture + markSuccess 必须在同一个 REQUIRED 事务中。
     */
    private <T> IdempotencyResult<T> executeOwnedTransactionally(
            String key,
            String routeKey,
            IdempotencyExecutionDefinition<T> definition,
            IdempotencyRecord record,
            IdempotencyCallback<T> callback,
            IdempotencyContext context,
            Instant startedAt,
            boolean lockFallback,
            boolean recoveryExecution) {

        IdempotencyPolicy policy = definition.policy();
        IdempotencyRepository repository = definition.repository();
        IdempotencyResultPolicy<T> resultPolicy = definition.resultPolicy();

        try {
            TransactionalCompletion<T> completion = transactionCoordinator.executeRequired(
                    transactionName(policy),
                    normalize(routeKey),
                    () -> {
                        T value = callback.doWithIdempotency(context);
                        String resultPayload = captureResult(value, resultPolicy);
                        IdempotencyWriteResult write = repository.markSuccess(
                                successRequest(
                                        key, policy, record, resultPayload, Instant.now(clock)));

                        if (write.getStatus() != IdempotencyWriteStatus.UPDATED) {
                            throw new CompletionRejectedException(write);
                        }
                        return new TransactionalCompletion<>(value, write.getRecord());
                    });

            publish(IdempotencyEventType.EXECUTION_SUCCESS, IdempotencyStage.COMPLETE_STATE,
                    policy, repository, null);
            return finish(
                    recoveryExecution ? IdempotencyResultStatus.RECOVERED
                            : IdempotencyResultStatus.EXECUTED,
                    IdempotencyStage.COMPLETE_STATE,
                    completion.value,
                    completion.record,
                    null,
                    policy,
                    repository,
                    startedAt,
                    lockFallback,
                    true);

        } catch (CompletionRejectedException rejected) {
            IdempotencyWriteResult write = rejected.write;
            if (write.getStatus() == IdempotencyWriteStatus.STALE_OWNER
                    || write.getStatus() == IdempotencyWriteStatus.ALREADY_FINAL) {
                publish(IdempotencyEventType.OWNERSHIP_LOST, IdempotencyStage.COMPLETE_STATE,
                        policy, repository, null);
                return finish(
                        IdempotencyResultStatus.OWNERSHIP_LOST,
                        IdempotencyStage.COMPLETE_STATE,
                        null,
                        write.getRecord(),
                        write.getError(),
                        policy,
                        repository,
                        startedAt,
                        lockFallback,
                        true);
            }

            publish(IdempotencyEventType.REPOSITORY_ERROR, IdempotencyStage.COMPLETE_STATE,
                    policy, repository, write.getError());
            return finish(
                    IdempotencyResultStatus.REPOSITORY_ERROR,
                    IdempotencyStage.COMPLETE_STATE,
                    null,
                    write.getRecord(),
                    write.getError(),
                    policy,
                    repository,
                    startedAt,
                    lockFallback,
                    true);

        } catch (ResultPolicyException resultError) {
            IdempotencyWriteResult failureWrite = persistFailure(
                    key,
                    policy,
                    repository,
                    record,
                    new IdempotencyFailureInfo(
                            "RESULT_POLICY_ERROR",
                            safeMessage(resultError.getCause()),
                            false,
                            Instant.now(clock)));
            attachProviderFailure(resultError, failureWrite);

            return finish(
                    IdempotencyResultStatus.RESULT_POLICY_ERROR,
                    IdempotencyStage.COMPLETE_STATE,
                    null,
                    failureWrite.getRecord() == null ? record : failureWrite.getRecord(),
                    resultError.getCause(),
                    policy,
                    repository,
                    startedAt,
                    lockFallback,
                    true);

        } catch (IdempotencyTransactionException transactionError) {
            return handleTransactionFailure(
                    key,
                    policy,
                    repository,
                    record,
                    transactionError,
                    startedAt,
                    lockFallback);

        } catch (Throwable businessError) {
            return handleBusinessFailure(
                    key,
                    policy,
                    repository,
                    record,
                    businessError,
                    startedAt,
                    lockFallback,
                    true);
        }
    }

    /**
     * 没有本地事务原子闭环时仍保留 owner/version CAS。
     */
    private <T> IdempotencyResult<T> executeOwnedWithoutBusinessTransaction(
            String key,
            IdempotencyExecutionDefinition<T> definition,
            IdempotencyRecord record,
            IdempotencyCallback<T> callback,
            IdempotencyContext context,
            Instant startedAt,
            boolean lockFallback,
            boolean recoveryExecution) {

        IdempotencyPolicy policy = definition.policy();
        IdempotencyRepository repository = definition.repository();

        try {
            T value = callback.doWithIdempotency(context);
            String resultPayload = captureResult(value, definition.resultPolicy());
            IdempotencyWriteResult write = repository.markSuccess(
                    successRequest(key, policy, record, resultPayload, Instant.now(clock)));

            if (write.getStatus() == IdempotencyWriteStatus.UPDATED) {
                publish(IdempotencyEventType.EXECUTION_SUCCESS, IdempotencyStage.COMPLETE_STATE,
                        policy, repository, null);
                return finish(
                        recoveryExecution ? IdempotencyResultStatus.RECOVERED
                                : IdempotencyResultStatus.EXECUTED,
                        IdempotencyStage.COMPLETE_STATE,
                        value,
                        write.getRecord(),
                        null,
                        policy,
                        repository,
                        startedAt,
                        lockFallback,
                        false);
            }

            if (write.getStatus() == IdempotencyWriteStatus.STALE_OWNER
                    || write.getStatus() == IdempotencyWriteStatus.ALREADY_FINAL) {
                publish(IdempotencyEventType.OWNERSHIP_LOST, IdempotencyStage.COMPLETE_STATE,
                        policy, repository, null);
                return finish(
                        IdempotencyResultStatus.OWNERSHIP_LOST,
                        IdempotencyStage.COMPLETE_STATE,
                        null,
                        write.getRecord(),
                        null,
                        policy,
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
                    policy,
                    repository,
                    startedAt,
                    lockFallback,
                    false);

        } catch (ResultPolicyException resultError) {
            // 非事务模式下业务副作用可能已经发生，禁止把该错误标成 retryable，
            // 否则自动恢复可能再次执行真实业务。
            IdempotencyWriteResult failureWrite = persistFailure(
                    key,
                    policy,
                    repository,
                    record,
                    new IdempotencyFailureInfo(
                            "RESULT_POLICY_ERROR",
                            safeMessage(resultError.getCause()),
                            false,
                            Instant.now(clock)));

            return finish(
                    IdempotencyResultStatus.RESULT_POLICY_ERROR,
                    IdempotencyStage.COMPLETE_STATE,
                    null,
                    failureWrite.getRecord() == null ? record : failureWrite.getRecord(),
                    resultError.getCause(),
                    policy,
                    repository,
                    startedAt,
                    lockFallback,
                    false);

        } catch (Throwable businessError) {
            return handleBusinessFailure(
                    key,
                    policy,
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
            IdempotencyPolicy policy,
            IdempotencyRecord record,
            String resultPayload,
            Instant now) {
        return new IdempotencySuccessRequest(
                policy.getNamespace(),
                key,
                record.getOwnerToken(),
                record.getVersion(),
                resultPayload,
                policy.getMode(),
                policy.getIdempotencyWindow(),
                policy.getWindowPolicy(),
                policy.getRecordRetentionTtl(),
                now);
    }

    private <T> String captureResult(
            T value,
            IdempotencyResultPolicy<T> resultPolicy)
            throws ResultPolicyException {

        if (!resultPolicy.storesPayload()) {
            return null;
        }

        try {
            String captured = resultPolicy.capture(value);
            if (captured == null) {
                throw new IllegalStateException(
                        resultPolicy.type() + " result policy returned null stored value");
            }
            return StoredResultEnvelope.encode(resultPolicy.type(), captured);
        } catch (Exception error) {
            throw new ResultPolicyException(error);
        }
    }

    private <T> IdempotencyResult<T> handleBusinessFailure(
            String key,
            IdempotencyPolicy policy,
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
                key, policy, repository, record, failure);

        attachProviderFailure(businessError, write);
        publish(IdempotencyEventType.EXECUTION_FAILED, IdempotencyStage.EXECUTE,
                policy, repository, businessError);

        return finish(
                IdempotencyResultStatus.EXECUTION_FAILED,
                IdempotencyStage.EXECUTE,
                null,
                write.getRecord() == null ? record : write.getRecord(),
                businessError,
                policy,
                repository,
                startedAt,
                lockFallback,
                transactionApplied);
    }

    private <T> IdempotencyResult<T> handleTransactionFailure(
            String key,
            IdempotencyPolicy policy,
            IdempotencyRepository repository,
            IdempotencyRecord record,
            IdempotencyTransactionException transactionError,
            Instant startedAt,
            boolean lockFallback) {

        if (transactionError.outcome() == IdempotencyTransactionOutcome.COMMIT_UNKNOWN) {
            publish(IdempotencyEventType.TRANSACTION_COMMIT_UNKNOWN, IdempotencyStage.TRANSACTION,
                    policy, repository, transactionError);
            return finish(
                    IdempotencyResultStatus.TRANSACTION_COMMIT_UNKNOWN,
                    IdempotencyStage.TRANSACTION,
                    null,
                    record,
                    transactionError,
                    policy,
                    repository,
                    startedAt,
                    lockFallback,
                    true);
        }

        IdempotencyWriteResult write = persistFailure(
                key,
                policy,
                repository,
                record,
                new IdempotencyFailureInfo(
                        "TRANSACTION_" + transactionError.outcome().name(),
                        safeMessage(transactionError),
                        true,
                        Instant.now(clock)));
        attachProviderFailure(transactionError, write);

        publish(IdempotencyEventType.TRANSACTION_FAILED, IdempotencyStage.TRANSACTION,
                policy, repository, transactionError);
        return finish(
                IdempotencyResultStatus.TRANSACTION_FAILED,
                IdempotencyStage.TRANSACTION,
                null,
                write.getRecord() == null ? record : write.getRecord(),
                transactionError,
                policy,
                repository,
                startedAt,
                lockFallback,
                true);
    }

    private IdempotencyWriteResult persistFailure(
            String key,
            IdempotencyPolicy policy,
            IdempotencyRepository repository,
            IdempotencyRecord record,
            IdempotencyFailureInfo failure) {
        return repository.markFailed(
                new IdempotencyFailureRequest(
                        policy.getNamespace(),
                        key,
                        record.getOwnerToken(),
                        record.getVersion(),
                        failure,
                        policy.getMode(),
                        policy.getIdempotencyWindow(),
                        policy.getWindowPolicy(),
                        policy.getRecordRetentionTtl(),
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

    private String transactionName(IdempotencyPolicy policy) {
        return "idempotency-business:" + policy.getNamespace();
    }

    private String safeMessage(Throwable error) {
        if (error == null) {
            return null;
        }
        String message = error.getMessage();
        return message == null || message.isBlank()
                ? error.getClass().getSimpleName()
                : message;
    }

    private <R> StateInvocation<R> invokeWithOptionalLock(
            IdempotencyPolicy policy,
            IdempotencyRepository repository,
            String routeKey,
            String key,
            StateOperation<R> operation) {

        IdempotencyLockOptions lock = policy.getLockOptions();
        if (!lock.isEnabled()) {
            return StateInvocation.direct(operation.invoke());
        }

        if (lockClient == null) {
            if (lock.isFallbackToStateOnFailure()) {
                publish(IdempotencyEventType.LOCK_FALLBACK, IdempotencyStage.LOCK,
                        policy, repository, null);
                return StateInvocation.fallback(operation.invoke());
            }
            return StateInvocation.lockRejected(
                    new IllegalStateException(
                            "lock enabled but DistributedLockClient unavailable"));
        }

        LockWaitStrategy waitStrategy = lock.getWaitTime().isZero()
                ? LockWaitStrategy.NO_WAIT : LockWaitStrategy.BACKOFF;

        LockOptions lockOptions = LockOptions.builder()
                .namespace("idempotency:" + policy.getNamespace())
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
                    policy, repository, result.error().orElse(null));
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
     * 历史 SUCCESS 的结果回放。
     */
    private <T> IdempotencyResult<T> replay(
            IdempotencyRecord record,
            IdempotencyResultPolicy<T> resultPolicy,
            boolean lockFallback) {

        IdempotencyResultPolicy<T> resolved = resultPolicy == null
                ? IdempotencyResultPolicies.none()
                : resultPolicy;

        if (resolved.type() == IdempotencyResultPolicyType.NONE) {
            return IdempotencyResult.<T>builder()
                    .status(IdempotencyResultStatus.REPLAYED)
                    .stage(IdempotencyStage.REPLAY)
                    .record(record)
                    .lockFallback(lockFallback)
                    .build();
        }

        String payload = record == null ? null : record.getResultPayload();
        if (payload == null || payload.isBlank()) {
            return IdempotencyResult.<T>builder()
                    .status(IdempotencyResultStatus.RESULT_REPLAY_UNAVAILABLE)
                    .stage(IdempotencyStage.REPLAY)
                    .record(record)
                    .error(new IllegalStateException(
                            "historical SUCCESS has no stored result for "
                                    + resolved.type() + " replay"))
                    .lockFallback(lockFallback)
                    .build();
        }

        try {
            StoredResultEnvelope.Decoded decoded = StoredResultEnvelope.decode(payload);
            if (decoded.type() != resolved.type()) {
                return IdempotencyResult.<T>builder()
                        .status(IdempotencyResultStatus.RESULT_POLICY_MISMATCH)
                        .stage(IdempotencyStage.REPLAY)
                        .record(record)
                        .error(new IllegalStateException(
                                "stored result policy is " + decoded.type()
                                        + " but current request uses " + resolved.type()))
                        .lockFallback(lockFallback)
                        .build();
            }

            return IdempotencyResult.<T>builder()
                    .status(IdempotencyResultStatus.REPLAYED)
                    .stage(IdempotencyStage.REPLAY)
                    .value(resolved.replay(decoded.value()))
                    .record(record)
                    .lockFallback(lockFallback)
                    .build();

        } catch (Exception error) {
            return IdempotencyResult.<T>builder()
                    .status(IdempotencyResultStatus.RESULT_POLICY_ERROR)
                    .stage(IdempotencyStage.REPLAY)
                    .record(record)
                    .error(error)
                    .lockFallback(lockFallback)
                    .build();
        }
    }

    private <T> IdempotencyResult<T> invalid(Throwable error) {
        return IdempotencyResult.<T>builder()
                .status(IdempotencyResultStatus.VALIDATION_FAILED)
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
            IdempotencyPolicy policy,
            IdempotencyRepository repository,
            Instant startedAt,
            boolean lockFallback,
            boolean transactionApplied) {

        metrics.recordExecution(
                policy.getMode(),
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
                .transactionApplied(transactionApplied)
                .build();
    }

    private void publish(
            IdempotencyEventType type,
            IdempotencyStage stage,
            IdempotencyPolicy policy,
            IdempotencyRepository repository,
            Throwable error) {
        events.publish(new IdempotencyEvent(
                type,
                stage,
                policy.getMode(),
                repository.providerName(),
                Instant.now(clock),
                error));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    @FunctionalInterface
    private interface StateOperation<R> {
        R invoke();
    }

    private static final class StateInvocation<R> {
        private final R result;
        private final boolean lockFallback;
        private final boolean lockRejected;
        private final Throwable error;

        private StateInvocation(
                R result,
                boolean lockFallback,
                boolean lockRejected,
                Throwable error) {
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

    private static final class CompletionRejectedException extends RuntimeException {
        private final IdempotencyWriteResult write;

        private CompletionRejectedException(IdempotencyWriteResult write) {
            super("idempotency completion rejected: "
                    + (write == null ? "null" : write.getStatus()));
            this.write = Objects.requireNonNull(write, "write must not be null");
        }
    }

    private static final class TransactionalCompletion<T> {
        private final T value;
        private final IdempotencyRecord record;

        private TransactionalCompletion(T value, IdempotencyRecord record) {
            this.value = value;
            this.record = record;
        }
    }

    private static final class ResultPolicyException extends Exception {
        private ResultPolicyException(Throwable cause) {
            super(cause);
        }
    }
}
