package com.xjtu.iron.idempotent.core.repository;

import com.xjtu.iron.idempotent.api.policy.IdempotencyMode;
import com.xjtu.iron.idempotent.api.repository.IdempotencyRepository;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository 注册表。
 *
 * <p>WINDOWED 与 DURABLE 分别拥有默认 Provider；具体能力由 Repository capabilities 声明，
 * Core 不通过 providerName 猜 Redis/JDBC 行为。</p>
 */
public final class DefaultIdempotencyRepositoryRegistry
        implements IdempotencyRepositoryRegistry {

    private final Map<String, IdempotencyRepository> repositories = new LinkedHashMap<>();
    private final Map<IdempotencyMode, String> defaults = new EnumMap<>(IdempotencyMode.class);

    public DefaultIdempotencyRepositoryRegistry(
            List<IdempotencyRepository> repositoryList,
            String windowedDefault,
            String durableDefault) {

        if (repositoryList != null) {
            for (IdempotencyRepository repository : repositoryList) {
                IdempotencyRepository old = repositories.put(
                        repository.providerName(), repository);
                if (old != null) {
                    throw new IllegalArgumentException(
                            "duplicate idempotency repository: " + repository.providerName());
                }
            }
        }

        defaults.put(IdempotencyMode.WINDOWED, windowedDefault);
        defaults.put(IdempotencyMode.DURABLE, durableDefault);
    }

    @Override
    public IdempotencyRepository resolve(IdempotencyMode mode, String requestedName) {
        IdempotencyMode canonical = mode == null
                ? IdempotencyMode.DURABLE
                : mode.canonical();

        String name = requestedName == null || requestedName.isBlank()
                ? defaults.get(canonical)
                : requestedName;

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("no default repository for mode: " + canonical);
        }

        IdempotencyRepository repository = repositories.get(name);
        if (repository == null) {
            throw new IllegalArgumentException("idempotency repository not found: " + name);
        }

        if (!repository.capabilities().supports(canonical)) {
            throw new IllegalArgumentException(
                    "repository " + name + " does not support " + canonical);
        }
        return repository;
    }
}
