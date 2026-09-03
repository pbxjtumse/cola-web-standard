package com.xjtu.iron.idempotent.core.operation;

import com.xjtu.iron.idempotent.api.operation.IdempotencyOperations;
import com.xjtu.iron.idempotent.api.operation.acquire.IdempotencyOperationAcquireCommand;
import com.xjtu.iron.idempotent.api.operation.acquire.IdempotencyOperationAcquireResult;
import com.xjtu.iron.idempotent.api.operation.acquire.IdempotencyOperationAcquireStatus;
import com.xjtu.iron.idempotent.api.operation.write.IdempotencyCompletionCommand;
import com.xjtu.iron.idempotent.api.operation.write.IdempotencyFailureCommand;
import com.xjtu.iron.idempotent.api.operation.write.IdempotencyOperationWriteResult;
import com.xjtu.iron.idempotent.api.operation.write.IdempotencyOperationWriteStatus;
import com.xjtu.iron.idempotent.api.policy.IdempotencyPolicy;
import com.xjtu.iron.idempotent.api.repository.IdempotencyRepository;
import com.xjtu.iron.idempotent.api.repository.acquire.IdempotencyAcquireRequest;
import com.xjtu.iron.idempotent.api.repository.acquire.IdempotencyAcquireResult;
import com.xjtu.iron.idempotent.api.repository.write.IdempotencyDiscardRequest;
import com.xjtu.iron.idempotent.api.repository.write.IdempotencyFailureRequest;
import com.xjtu.iron.idempotent.api.repository.write.IdempotencySuccessRequest;
import com.xjtu.iron.idempotent.api.repository.write.IdempotencyWriteResult;
import com.xjtu.iron.idempotent.api.repository.write.IdempotencyWriteStatus;
import com.xjtu.iron.idempotent.api.storage.IdempotencyStorageContext;
import com.xjtu.iron.idempotent.core.policy.IdempotencyPolicyRegistry;
import com.xjtu.iron.idempotent.core.repository.IdempotencyRepositoryRegistry;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * 面向其他技术组件的低层幂等状态操作实现。
 *
 * <p>这里故意不执行业务 callback，也不持有分布式锁。message/task/workflow 自己拥有业务编排与事务边界，
 * 本类只负责把稳定的 Policy + StorageContext 转换成 Repository 原子状态命令。</p>
 *
 * <p>正确性仍由 Repository 的 UNIQUE / 行锁 / Lua / owner-version CAS 保证。</p>
 */
public final class DefaultIdempotencyOperations implements IdempotencyOperations {

    private final IdempotencyRepositoryRegistry repositoryRegistry;
    private final IdempotencyPolicyRegistry policyRegistry;
    private final Clock clock;

    public DefaultIdempotencyOperations(IdempotencyRepositoryRegistry repositoryRegistry, IdempotencyPolicyRegistry policyRegistry, Clock clock) {
        this.repositoryRegistry = Objects.requireNonNull(repositoryRegistry, "repositoryRegistry must not be null");
        this.policyRegistry = Objects.requireNonNull(policyRegistry, "policyRegistry must not be null");
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    public IdempotencyOperationAcquireResult acquire(IdempotencyOperationAcquireCommand command) {
        validateAcquire(command);
        IdempotencyPolicy policy = resolvePolicy(command.getPolicyName(), command.getPolicy());
        IdempotencyRepository repository = resolveRepository(policy);

        IdempotencyAcquireResult result = repository.tryAcquire(new IdempotencyAcquireRequest(
                command.getStorageContext(), policy.getNamespace(), command.getKey(), normalize(command.getRequestHash()),
                normalize(command.getRouteKey()), command.getOwnerToken(), policy.getMode(), policy.getProcessingTimeout(),
                policy.getIdempotencyWindow(), policy.getWindowPolicy(), policy.getRecordRetentionTtl(),
                policy.getRecoveryPolicy().getMode(), Instant.now(clock)));

        return switch (result.getStatus()) {
            case ACQUIRED -> IdempotencyOperationAcquireResult.of(IdempotencyOperationAcquireStatus.ACQUIRED, result.getRecord());
            case SUCCESS -> IdempotencyOperationAcquireResult.of(IdempotencyOperationAcquireStatus.DUPLICATE_SUCCESS, result.getRecord());
            case DISCARDED -> IdempotencyOperationAcquireResult.of(IdempotencyOperationAcquireStatus.DUPLICATE_DISCARDED, result.getRecord());
            case PROCESSING_ACTIVE -> IdempotencyOperationAcquireResult.of(IdempotencyOperationAcquireStatus.PROCESSING, result.getRecord());
            case PROCESSING_EXPIRED -> IdempotencyOperationAcquireResult.of(IdempotencyOperationAcquireStatus.PROCESSING_EXPIRED, result.getRecord());
            case FAILED_RETRYABLE -> IdempotencyOperationAcquireResult.of(IdempotencyOperationAcquireStatus.FAILED_RETRYABLE, result.getRecord());
            case FAILED_FINAL -> IdempotencyOperationAcquireResult.of(IdempotencyOperationAcquireStatus.FAILED_FINAL, result.getRecord());
            case KEY_CONFLICT -> IdempotencyOperationAcquireResult.of(IdempotencyOperationAcquireStatus.KEY_CONFLICT, result.getRecord());
            case PROVIDER_ERROR -> IdempotencyOperationAcquireResult.storageError(result.getError());
        };
    }

    @Override
    public IdempotencyOperationWriteResult markSuccess(IdempotencyCompletionCommand command) {
        validateCompletion(command);
        IdempotencyPolicy policy = resolvePolicy(command.getPolicyName(), command.getPolicy());
        IdempotencyRepository repository = resolveRepository(policy);
        return mapWrite(repository.markSuccess(new IdempotencySuccessRequest(
                command.getStorageContext(), policy.getNamespace(), command.getKey(), command.getOwnerToken(), command.getVersion(),
                command.getResultPayload(), policy.getMode(), policy.getIdempotencyWindow(), policy.getWindowPolicy(),
                policy.getRecordRetentionTtl(), Instant.now(clock))));
    }

    @Override
    public IdempotencyOperationWriteResult markFailed(IdempotencyFailureCommand command) {
        validateFailure(command);
        IdempotencyPolicy policy = resolvePolicy(command.getPolicyName(), command.getPolicy());
        IdempotencyRepository repository = resolveRepository(policy);
        return mapWrite(repository.markFailed(new IdempotencyFailureRequest(
                command.getStorageContext(), policy.getNamespace(), command.getKey(), command.getOwnerToken(), command.getVersion(),
                command.getFailure(), policy.getMode(), policy.getIdempotencyWindow(), policy.getWindowPolicy(),
                policy.getRecordRetentionTtl(), Instant.now(clock))));
    }

    @Override
    public IdempotencyOperationWriteResult markDiscarded(IdempotencyCompletionCommand command) {
        validateCompletion(command);
        IdempotencyPolicy policy = resolvePolicy(command.getPolicyName(), command.getPolicy());
        IdempotencyRepository repository = resolveRepository(policy);
        return mapWrite(repository.markDiscarded(new IdempotencyDiscardRequest(
                command.getStorageContext(), policy.getNamespace(), command.getKey(), command.getOwnerToken(), command.getVersion(),
                command.getResultPayload(), policy.getMode(), policy.getIdempotencyWindow(), policy.getWindowPolicy(),
                policy.getRecordRetentionTtl(), Instant.now(clock))));
    }

    private IdempotencyOperationWriteResult mapWrite(IdempotencyWriteResult result) {
        if (result.getStatus() == IdempotencyWriteStatus.PROVIDER_ERROR) {
            return IdempotencyOperationWriteResult.storageError(result.getError());
        }
        IdempotencyOperationWriteStatus status = switch (result.getStatus()) {
            case UPDATED -> IdempotencyOperationWriteStatus.UPDATED;
            case STALE_OWNER -> IdempotencyOperationWriteStatus.STALE_OWNER;
            case ALREADY_FINAL -> IdempotencyOperationWriteStatus.ALREADY_FINAL;
            case NOT_FOUND -> IdempotencyOperationWriteStatus.NOT_FOUND;
            case PROVIDER_ERROR -> throw new IllegalStateException("provider error must be handled before status mapping");
        };
        return IdempotencyOperationWriteResult.of(status, result.getRecord());
    }

    private IdempotencyPolicy resolvePolicy(String policyName, IdempotencyPolicy inlinePolicy) {
        IdempotencyPolicy policy = policyRegistry.resolve(policyName, inlinePolicy);
        policy.validate();
        return policy;
    }

    private IdempotencyRepository resolveRepository(IdempotencyPolicy policy) {
        return repositoryRegistry.resolve(policy.getMode(), policy.getRepositoryName());
    }

    private void validateAcquire(IdempotencyOperationAcquireCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        validateIdentity(command.getStorageContext(), command.getKey(), command.getOwnerToken());
    }

    private void validateCompletion(IdempotencyCompletionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        validateIdentity(command.getStorageContext(), command.getKey(), command.getOwnerToken());
        if (command.getVersion() <= 0) throw new IllegalArgumentException("version must be positive");
    }

    private void validateFailure(IdempotencyFailureCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        validateIdentity(command.getStorageContext(), command.getKey(), command.getOwnerToken());
        Objects.requireNonNull(command.getFailure(), "failure must not be null");
        if (command.getVersion() <= 0) throw new IllegalArgumentException("version must be positive");
    }

    private void validateIdentity(IdempotencyStorageContext storage, String key, String ownerToken) {
        Objects.requireNonNull(storage, "storageContext must not be null");
        if (key == null || key.isBlank()) throw new IllegalArgumentException("idempotency key must not be blank");
        if (ownerToken == null || ownerToken.isBlank()) throw new IllegalArgumentException("ownerToken must not be blank");
    }

    private String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
