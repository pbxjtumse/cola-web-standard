package com.xjtu.iron.idempotent.core;

import com.xjtu.iron.idempotent.api.IdempotencyMode;
import com.xjtu.iron.idempotent.api.IdempotencyRecoveryQueryService;
import com.xjtu.iron.idempotent.api.repository.IdempotencyRecoveryCandidate;
import com.xjtu.iron.idempotent.api.repository.IdempotencyRecoveryQuery;
import com.xjtu.iron.idempotent.api.repository.IdempotencyRecoveryRepository;
import com.xjtu.iron.idempotent.api.repository.IdempotencyRepository;

import java.util.List;
import java.util.Objects;

/** 默认恢复候选查询服务。 */
public final class DefaultIdempotencyRecoveryQueryService
        implements IdempotencyRecoveryQueryService {

    private final IdempotencyRepositoryRegistry registry;

    public DefaultIdempotencyRecoveryQueryService(IdempotencyRepositoryRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
    }

    @Override
    public List<IdempotencyRecoveryCandidate> findCandidates(
            IdempotencyMode mode,
            String repositoryName,
            IdempotencyRecoveryQuery query) {
        Objects.requireNonNull(mode, "mode must not be null");
        Objects.requireNonNull(query, "query must not be null");

        IdempotencyRepository repository = registry.resolve(mode, repositoryName);
        if (!(repository instanceof IdempotencyRecoveryRepository recoveryRepository)) {
            return List.of();
        }
        return recoveryRepository.findRecoveryCandidates(query);
    }
}
