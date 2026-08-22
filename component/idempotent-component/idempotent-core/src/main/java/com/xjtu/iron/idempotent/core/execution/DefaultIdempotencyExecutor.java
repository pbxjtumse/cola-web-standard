package com.xjtu.iron.idempotent.core.execution;

import com.xjtu.iron.idempotent.core.repository.IdempotencyRepositoryRegistry;
import com.xjtu.iron.idempotent.core.policy.IdempotencyPolicyRegistry;
import com.xjtu.iron.idempotent.core.owner.IdempotencyOwnerTokenGenerator;

import com.xjtu.iron.distributed.lock.api.client.DistributedLockClient;
import com.xjtu.iron.distributed.lock.api.client.LockCallback;
import com.xjtu.iron.distributed.lock.api.model.LockOptions;
import com.xjtu.iron.distributed.lock.api.model.LockResult;
import com.xjtu.iron.distributed.lock.api.LockWaitStrategy;
import com.xjtu.iron.idempotent.api.execution.*;
import com.xjtu.iron.idempotent.api.policy.*;
import com.xjtu.iron.idempotent.api.recovery.*;
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
 * 默认幂等执行器，也是理解整个组件最重要的主编排类。
 *
 * <p>阅读本类时先记住：Executor 不自己实现数据库并发控制，它负责把各层按固定顺序串起来。</p>
 *
 * <pre>
 * 01 Request 校验
 * 02 Policy / Repository / ResultPolicy 解析并冻结为 ExecutionDefinition
 * 03 生成本次 candidate ownerToken
 * 04 optional DistributedLock：只包 tryAcquire / tryRecover，作用是降低热点竞争
 * 05 Repository 原子状态转换：UNIQUE / SELECT FOR UPDATE / Lua / CAS，真正决定 generation owner
 * 06 StateMachine：把 Repository 已确认的事实翻译成 EXECUTE / REPLAY / RETURN
 * 07 EXECUTE：创建 IdempotencyContext，进入业务 callback
 * 08 transaction-aware JDBC：Tx-B REQUIRED = Business + ResultPolicy.capture + markSuccess
 * 09 最终 markSuccess(ownerToken, version) CAS：旧 generation 在这里被拒绝
 * 10 失败：Tx-B rollback 后由 Tx-C REQUIRES_NEW 记录 FAILED
 * 11 历史 SUCCESS：不再次执行 callback，只按 ResultPolicy 做结果回放
 * 12 Recovery：expectedOwner + expectedVersion 二次 CAS 成功后，复用同一 executeOwned 主链
 * </pre>
 *
 * <p>WINDOWED 与 DURABLE 复用同一套 Core 编排；差异由 Policy 与 Repository capabilities 表达。
 * DistributedLock 是竞争优化层，Repository 原子转换才是幂等正确性的根基。</p>
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
        this.repositoryRegistry = Objects.requireNonNull(repositoryRegistry, "repositoryRegistry must not be null");
        this.policyRegistry = Objects.requireNonNull(policyRegistry, "policyRegistry must not be null");
        this.ownerGenerator = Objects.requireNonNull(ownerGenerator, "ownerGenerator must not be null");
        this.failureClassifier = Objects.requireNonNull(failureClassifier, "failureClassifier must not be null");
        this.lockClient = lockClient;
        this.transactionCoordinator = transactionCoordinator;
        this.stateMachine = stateMachine == null
                ? new DefaultIdempotencyStateMachine() : stateMachine;
        this.events = events == null ? IdempotencyEventPublisher.noop() : events;
        this.metrics = metrics == null ? IdempotencyMetrics.noop() : metrics;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    /**
     * 普通请求入口。
     *
     * <p>这个方法只负责“第一次请求 / 普通重复请求”的去重判断，不会因为 PROCESSING_EXPIRED 或
     * FAILED_RETRYABLE 就擅自接管旧 generation；故障接管必须走 {@link #recover}。</p>
     */
    @Override
    public <T> IdempotencyResult<T> execute(IdempotencyRequest request, IdempotencyResultPolicy<T> resultPolicy,
                                             IdempotencyCallback<T> callback) {

        Objects.requireNonNull(callback, "callback must not be null");
        Instant startedAt = Instant.now(clock);

        // [01-02] 先校验请求，再一次性解析 Policy / Repository / ResultPolicy。
        // prepare() 返回不可变 ExecutionDefinition，后续主流程不再反复读取可选配置。
        IdempotencyExecutionDefinition<T> definition;
        try {
            validateNormalRequest(request);
            definition = prepare(request.getPolicyName(), request.getPolicy(), resultPolicy);
        } catch (RuntimeException error) {
            return invalid(error);
        }

        IdempotencyPolicy policy = definition.policy();
        IdempotencyRepository repository = definition.repository();

        // [03] 为“这一次尝试成为当前 generation owner”生成候选 ownerToken。
        // 它不是线程 ID，也不是 distributed-lock fencingToken；真正是否成为 owner 要由 Repository 原子抢占决定。
        String ownerToken = ownerGenerator.generate(policy.getNamespace(), request.getKey());

        // 把 Request + Policy 解析成 Repository 所需的原子抢占参数。processingTimeout 是当前 generation 的执行租约，
        // idempotencyWindow / retention 只在 WINDOWED 生命周期中起作用。
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

        publish(IdempotencyEventType.ACQUIRE_ATTEMPT, IdempotencyStage.ACQUIRE_STATE, policy, repository, null);

        // [04-05] Lock 只包状态抢占短临界区。即使 Lock 关闭或按策略 fallback，Repository 仍必须独立保证原子正确性。
        StateInvocation<IdempotencyAcquireResult> invocation = invokeWithOptionalLock(
                policy, repository, request.getRouteKey(), request.getKey(), () -> repository.tryAcquire(acquireRequest));

        if (invocation.lockRejected) {
            return lockRejected(invocation.error);
        }

        IdempotencyAcquireResult acquire = invocation.result;
        metrics.recordAcquire(policy.getMode(), repository.providerName(), acquire.getStatus().name());

        // [06] Repository 已经完成原子判断后，StateMachine 才把事实翻译成 EXECUTE / REPLAY / RETURN。
        // StateMachine 不查数据库、不加锁，也不做 CAS。
        IdempotencyStateDecision decision = stateMachine.onAcquire(acquire.getStatus());
        return applyAcquireDecision(decision, request, definition, acquire, callback, startedAt, invocation.lockFallback);
    }

    /**
     * 显式 Recovery 入口。
     *
     * <p>扫描器只能发现 candidate，不能直接授予执行权。真正 recover 时必须带回扫描时看到的 expectedOwnerToken /
     * expectedVersion，由 Repository 再做一次原子校验；只有 RECOVERY_ACQUIRED 才会进入业务执行。</p>
     */
    @Override
    public <T> IdempotencyResult<T> recover(IdempotencyRecoveryRequest request, IdempotencyResultPolicy<T> resultPolicy,
                                             IdempotencyCallback<T> callback) {

        Objects.requireNonNull(callback, "callback must not be null");
        Instant startedAt = Instant.now(clock);

        IdempotencyExecutionDefinition<T> definition;
        try {
            validateRecoveryRequest(request);
            definition = prepare(request.getPolicyName(), request.getPolicy(), resultPolicy);
        } catch (RuntimeException error) {
            return invalid(error);
        }

        IdempotencyPolicy policy = definition.policy();
        IdempotencyRepository repository = definition.repository();
        IdempotencyRecoveryPolicy recoveryPolicy = policy.getRecoveryPolicy();

        // Recovery 是受 Policy 控制的可靠任务能力，不允许普通请求线程隐式开启。

        if (!recoveryPolicy.isExternalTaskEnabled()) {
            return simple(
                    IdempotencyResultStatus.RECOVERY_NOT_ALLOWED,
                    IdempotencyStage.RECOVER_STATE,
                    null,
                    new IllegalStateException("recovery policy does not enable EXTERNAL_TASK"),
                    false);
        }

        // 为“下一代 generation”生成新的 ownerToken。注意：这里只是候选新 owner，最终仍要通过 tryRecover CAS。
        String newOwner = ownerGenerator.generate(policy.getNamespace(), request.getKey());
        IdempotencyRecoveryAcquireRequest recoveryRequest = new IdempotencyRecoveryAcquireRequest(
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

        publish(IdempotencyEventType.RECOVERY_ATTEMPT, IdempotencyStage.RECOVER_STATE, policy, repository, null);

        // 二次 CAS 是 Recovery 安全性的核心：expected=A/10 但当前已经 B/11 时必须返回 STALE_CANDIDATE。
        StateInvocation<IdempotencyRecoveryResult> invocation = invokeWithOptionalLock(
                policy, repository, request.getRouteKey(), request.getKey(), () -> repository.tryRecover(recoveryRequest));

        if (invocation.lockRejected) {
            return lockRejected(invocation.error);
        }

        IdempotencyRecoveryResult recovery = invocation.result;
        IdempotencyStateDecision decision = stateMachine.onRecovery(recovery.getStatus());

        // RECOVERY_ACQUIRED 后不另造一套业务逻辑，而是复用 executeOwned()，从而与普通 ACQUIRED 共用 Tx-B/Tx-C/final CAS。
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
            return replay(recovery.getRecord(), definition.resultPolicy(), invocation.lockFallback);
        }

        Throwable error = recovery.getStatus() == IdempotencyRecoveryStatus.PROVIDER_ERROR
                ? recovery.getError() : null;
        return simple(decision.resultStatus(), IdempotencyStage.RECOVER_STATE, recovery.getRecord(), error, invocation.lockFallback);
    }

    /**
     * 把普通 tryAcquire 的状态机决策落到三条稳定分支：EXECUTE、REPLAY、RETURN。
     */
    private <T> IdempotencyResult<T> applyAcquireDecision(IdempotencyStateDecision decision, IdempotencyRequest request,
                                                           IdempotencyExecutionDefinition<T> definition,
                                                           IdempotencyAcquireResult acquire, IdempotencyCallback<T> callback,
                                                           Instant startedAt, boolean lockFallback) {

        if (decision.action() == IdempotencyStateAction.EXECUTE) {
            return executeOwned(request.getKey(), request.getRouteKey(), definition, acquire.getRecord(), callback,
                    startedAt, lockFallback, false);
        }
        if (decision.action() == IdempotencyStateAction.REPLAY) {
            return replay(acquire.getRecord(), definition.resultPolicy(), lockFallback);
        }

        Throwable error = acquire.getStatus() == IdempotencyAcquireStatus.PROVIDER_ERROR
                ? acquire.getError() : null;
        return simple(decision.resultStatus(), IdempotencyStage.ACQUIRE_STATE, acquire.getRecord(), error, lockFallback);
    }

    /**
     * 解析并冻结一次执行所需的稳定依赖。
     *
     * <p>这里做能力校验非常重要：例如调用方选择 SNAPSHOT/REFERENCE，但 Repository 不支持 result payload，
     * 应在真正抢占状态前直接失败，而不是等业务已经执行以后才发现无法保存结果。</p>
     */
    private <T> IdempotencyExecutionDefinition<T> prepare(String policyName, IdempotencyPolicy inlinePolicy,
                                                           IdempotencyResultPolicy<T> resultPolicy) {

        IdempotencyPolicy policy = policyRegistry.resolve(policyName, inlinePolicy);
        policy.validate();

        IdempotencyRepository repository = repositoryRegistry.resolve(policy.getMode(), policy.getRepositoryName());

        IdempotencyResultPolicy<T> resolvedResultPolicy = resultPolicy == null ? IdempotencyResultPolicies.none() : resultPolicy;

        if (resolvedResultPolicy.storesPayload() && !repository.capabilities().isResultPayloadSupported()) {
            throw new IllegalArgumentException("repository " + repository.providerName() + " does not support result payload storage");
        }

        return new IdempotencyExecutionDefinition<>(policy, repository, resolvedResultPolicy);
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
     * 只有 StateMachine 已经确认 EXECUTE 时才会进入这里。
     *
     * <p>此时 record 中的 ownerToken + version 就是本次 generation 身份。进入业务并不代表永久拥有完成权，
     * 最终 markSuccess/markFailed 仍必须带这两个条件，防止执行过程中被 Recovery 换代。</p>
     */
    private <T> IdempotencyResult<T> executeOwned(String key, String routeKey, IdempotencyExecutionDefinition<T> definition,
                                                   IdempotencyRecord record, IdempotencyCallback<T> callback,
                                                   Instant startedAt, boolean lockFallback, boolean recoveryExecution) {

        IdempotencyPolicy policy = definition.policy();
        IdempotencyRepository repository = definition.repository();

        publish(IdempotencyEventType.EXECUTION_STARTED, IdempotencyStage.EXECUTE, policy, repository, null);

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

        // 只有 transaction-component 已接入，且 Repository 能真正复用业务事务资源时，才允许宣称 Tx-B 原子闭环成立。
        boolean transactionApplied = transactionCoordinator != null
                && repository.capabilities().isBusinessTransactionParticipationSupported();

        if (transactionApplied) {
            return executeOwnedTransactionally(key, routeKey, definition, record, callback, context,
                    startedAt, lockFallback, recoveryExecution);
        }

        return executeOwnedWithoutBusinessTransaction(key, definition, record, callback, context,
                startedAt, lockFallback, recoveryExecution);
    }

    /**
     * Tx-B：Business + ResultPolicy.capture + markSuccess 必须在同一个 REQUIRED 本地事务中。
     *
     * <p>为什么 final CAS 失败要抛异常？因为旧 owner 可能已经执行了业务 SQL；如果这里只返回 OWNERSHIP_LOST 而让
     * 事务正常结束，旧 generation 的业务写仍会 COMMIT。抛 CompletionRejectedException 是为了强制整个 Tx-B rollback。</p>
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
                        // [07] 真正业务只在当前 generation 已经 ACQUIRED / RECOVERY_ACQUIRED 后执行。
                        T value = callback.doWithIdempotency(context);

                        // [08] 结果策略属于 SUCCESS 完成协议：capture 失败时本地业务也必须一起回滚。
                        String resultPayload = captureResult(value, resultPolicy);

                        // [09] 最终 ownerToken + version CAS 是“当前 generation 还能否提交”的最后一道门。
                        IdempotencyWriteResult write = repository.markSuccess(
                                successRequest(key, policy, record, resultPayload, Instant.now(clock)));
                        if (write.getStatus() != IdempotencyWriteStatus.UPDATED) {
                            throw new CompletionRejectedException(write);
                        }
                        return new TransactionalCompletion<>(value, write.getRecord());
                    });

            publish(IdempotencyEventType.EXECUTION_SUCCESS, IdempotencyStage.COMPLETE_STATE, policy, repository, null);
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
            // 这里已经从 Tx-B 中抛出，transaction-component 会先完成 rollback，再回到这里翻译领域结果。
            IdempotencyWriteResult write = rejected.write;
            if (write.getStatus() == IdempotencyWriteStatus.STALE_OWNER || write.getStatus() == IdempotencyWriteStatus.ALREADY_FINAL) {
                publish(IdempotencyEventType.OWNERSHIP_LOST, IdempotencyStage.COMPLETE_STATE, policy, repository, null);
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

            publish(IdempotencyEventType.REPOSITORY_ERROR, IdempotencyStage.COMPLETE_STATE, policy, repository, write.getError());
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
            // capture 属于完成协议的一部分。Tx-B 已回滚后，再用 Tx-C 将不可自动重试的 RESULT_POLICY_ERROR 落成 FAILED。
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
            return handleTransactionFailure(key, policy, repository, record, transactionError, startedAt, lockFallback);

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
     * 没有本地事务原子闭环时的执行路径。
     *
     * <p>典型是 Redis WINDOWED + MySQL 业务：Repository 的 owner/version CAS 仍然有效，但组件绝不声称
     * Redis SUCCESS 与业务数据库写处于一个本地事务。业务若存在外部副作用，必须额外依赖业务唯一约束/下游幂等。</p>
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
            IdempotencyWriteResult write = repository.markSuccess(successRequest(key, policy, record, resultPayload, Instant.now(clock)));

            if (write.getStatus() == IdempotencyWriteStatus.UPDATED) {
                publish(IdempotencyEventType.EXECUTION_SUCCESS, IdempotencyStage.COMPLETE_STATE, policy, repository, null);
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

            if (write.getStatus() == IdempotencyWriteStatus.STALE_OWNER || write.getStatus() == IdempotencyWriteStatus.ALREADY_FINAL) {
                publish(IdempotencyEventType.OWNERSHIP_LOST, IdempotencyStage.COMPLETE_STATE, policy, repository, null);
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

    /**
     * 把第一次真实业务结果转换成可持久化 payload。
     * NONE 不保存；SNAPSHOT 保存响应快照；REFERENCE 保存稳定业务引用。
     */
    private <T> String captureResult(T value, IdempotencyResultPolicy<T> resultPolicy) throws ResultPolicyException {

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

    /**
     * 业务 callback 失败后的统一收口。
     *
     * <p>Tx-B 路径下业务事务已经回滚；随后 persistFailure() 通过 Repository 的 Tx-C/原子写记录 FAILED。
     * failureRetryable 只表示“可靠 Recovery 可以考虑接管”，普通 execute() 仍不会自动重跑业务。</p>
     */
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
        IdempotencyWriteResult write = persistFailure(key, policy, repository, record, failure);

        attachProviderFailure(businessError, write);
        publish(IdempotencyEventType.EXECUTION_FAILED, IdempotencyStage.EXECUTE, policy, repository, businessError);

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

    /**
     * 事务基础设施失败与业务异常分开处理。
     *
     * <p>COMMIT_UNKNOWN 是最特殊的情况：数据库可能已经真正提交，所以绝不能立即 markFailed；
     * 否则会把“实际成功”人为覆盖成 FAILED。这里只返回未知结果，交给后续查询/对账/Recovery 收敛。</p>
     */
    private <T> IdempotencyResult<T> handleTransactionFailure(
            String key,
            IdempotencyPolicy policy,
            IdempotencyRepository repository,
            IdempotencyRecord record,
            IdempotencyTransactionException transactionError,
            Instant startedAt,
            boolean lockFallback) {

        if (transactionError.outcome() == IdempotencyTransactionOutcome.COMMIT_UNKNOWN) {
            publish(IdempotencyEventType.TRANSACTION_COMMIT_UNKNOWN, IdempotencyStage.TRANSACTION, policy, repository, transactionError);
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

        publish(IdempotencyEventType.TRANSACTION_FAILED, IdempotencyStage.TRANSACTION, policy, repository, transactionError);
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

    /**
     * 使用当前 ownerToken + version 写 FAILED。旧 generation 即使恢复抛错，也没有资格污染新 generation 的状态。
     */
    private IdempotencyWriteResult persistFailure(String key, IdempotencyPolicy policy, IdempotencyRepository repository,
                                                   IdempotencyRecord record, IdempotencyFailureInfo failure) {
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

    /**
     * 可选短锁包装器。
     *
     * <p>锁只做 contention reduction：把大量同 key 请求先在锁层收敛，减少同时冲击 Repository。
     * 正确性仍由 operation 内部的 tryAcquire/tryRecover 原子实现保证，所以允许按 Policy fallback 到 Repository。</p>
     */
    private <R> StateInvocation<R> invokeWithOptionalLock(IdempotencyPolicy policy, IdempotencyRepository repository,
                                                           String routeKey, String key, StateOperation<R> operation) {

        IdempotencyLockOptions lock = policy.getLockOptions();
        if (!lock.isEnabled()) {
            return StateInvocation.direct(operation.invoke());
        }

        if (lockClient == null) {
            if (lock.isFallbackToStateOnFailure()) {
                publish(IdempotencyEventType.LOCK_FALLBACK, IdempotencyStage.LOCK, policy, repository, null);
                return StateInvocation.fallback(operation.invoke());
            }
            return StateInvocation.lockRejected(new IllegalStateException("lock enabled but DistributedLockClient unavailable"));
        }

        LockWaitStrategy waitStrategy = lock.getWaitTime().isZero() ? LockWaitStrategy.NO_WAIT : LockWaitStrategy.BACKOFF;

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
        LockResult<R> result = lockClient.execute(lockName, lockOptions, (LockCallback<R>) handle -> operation.invoke());

        if (result.isSuccess() && result.value().isPresent()) {
            return StateInvocation.direct(result.value().get());
        }

        if (lock.isFallbackToStateOnFailure()) {
            publish(IdempotencyEventType.LOCK_FALLBACK, IdempotencyStage.LOCK, policy, repository, result.error().orElse(null));
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
     *
     * <p>Replay 与 Retry 完全不同：这里绝不会再次执行业务 callback。NONE 只返回成功语义；
     * SNAPSHOT 从历史快照恢复 T；REFERENCE 用稳定业务引用重新查询/组装 T。</p>
     */
    private <T> IdempotencyResult<T> replay(IdempotencyRecord record, IdempotencyResultPolicy<T> resultPolicy, boolean lockFallback) {

        IdempotencyResultPolicy<T> resolved = resultPolicy == null ? IdempotencyResultPolicies.none() : resultPolicy;

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

    // -------------------- 统一结果构造 / 观测辅助 --------------------

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

        metrics.recordExecution(policy.getMode(), repository.providerName(), status, Duration.between(startedAt, Instant.now(clock)));

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
        events.publish(new IdempotencyEvent(type, stage, policy.getMode(), repository.providerName(), Instant.now(clock), error));
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
