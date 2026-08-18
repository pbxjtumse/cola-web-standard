package com.xjtu.iron.idempotent.core.recovery;

import com.xjtu.iron.idempotent.core.repository.IdempotencyRepositoryRegistry;
import com.xjtu.iron.idempotent.core.policy.IdempotencyPolicyRegistry;

import com.xjtu.iron.idempotent.api.policy.IdempotencyPolicy;
import com.xjtu.iron.idempotent.api.recovery.IdempotencyRecoveryQueryService;
import com.xjtu.iron.idempotent.api.repository.recovery.IdempotencyRecoveryCandidate;
import com.xjtu.iron.idempotent.api.repository.recovery.IdempotencyRecoveryQuery;
import com.xjtu.iron.idempotent.api.repository.recovery.IdempotencyRecoveryRepository;
import com.xjtu.iron.idempotent.api.repository.IdempotencyRepository;

import java.util.List;
import java.util.Objects;

/**
 * 默认恢复候选查询服务。
 *
 * <p>只做候选查询，不做自动接管。真正接管仍必须调用
 * {@code IdempotencyExecutor.recover(...)}，由 Repository CAS 再次验证
 * expectedOwnerToken + expectedVersion，避免扫描结果过期。</p>
 */
public final class DefaultIdempotencyRecoveryQueryService
        implements IdempotencyRecoveryQueryService {

    private final IdempotencyRepositoryRegistry repositoryRegistry;
    private final IdempotencyPolicyRegistry policyRegistry;

    public DefaultIdempotencyRecoveryQueryService(
            IdempotencyRepositoryRegistry repositoryRegistry,
            IdempotencyPolicyRegistry policyRegistry) {
        this.repositoryRegistry = Objects.requireNonNull(
                repositoryRegistry, "repositoryRegistry must not be null");
        this.policyRegistry = Objects.requireNonNull(
                policyRegistry, "policyRegistry must not be null");
    }

    @Override
    public List<IdempotencyRecoveryCandidate> findCandidates(
            String policyName,
            IdempotencyRecoveryQuery query) {
        Objects.requireNonNull(policyName, "policyName must not be null");
        Objects.requireNonNull(query, "query must not be null");

        IdempotencyPolicy policy = policyRegistry.resolve(policyName, null);
        IdempotencyRepository repository = repositoryRegistry.resolve(
                policy.getMode(), policy.getRepositoryName());

        // policyName 入口由 Policy 统一决定 namespace，避免任务配置和正常执行配置漂移。
        IdempotencyRecoveryQuery effectiveQuery = new IdempotencyRecoveryQuery(
                policy.getNamespace(),
                query.getRouteKey(),
                query.getNow(),
                query.getLimit());
        return query(repository, effectiveQuery);
    }

    private List<IdempotencyRecoveryCandidate> query(
            IdempotencyRepository repository,
            IdempotencyRecoveryQuery query) {
        if (!repository.capabilities().isRecoveryQuerySupported()
                || !(repository instanceof IdempotencyRecoveryRepository recoveryRepository)) {
            return List.of();
        }
        return recoveryRepository.findRecoveryCandidates(query);
    }
}
