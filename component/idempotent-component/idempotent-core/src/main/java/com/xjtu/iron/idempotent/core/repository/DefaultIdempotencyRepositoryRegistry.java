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
 * <p>WINDOWED 与 DURABLE 可以拥有不同默认 Provider，但 Core 不把 WINDOWED 写死成 Redis、DURABLE 写死成 JDBC。
 * 真正能否承载某种语义由 RepositoryCapabilities 显式声明。</p>
 */
public final class DefaultIdempotencyRepositoryRegistry implements IdempotencyRepositoryRegistry {

    private final Map<String, IdempotencyRepository> repositories = new LinkedHashMap<>();
    private final Map<IdempotencyMode, String> defaults = new EnumMap<>(IdempotencyMode.class);

    public DefaultIdempotencyRepositoryRegistry(List<IdempotencyRepository> repositoryList, String windowedDefault, String durableDefault) {
        if (repositoryList != null) {
            for (IdempotencyRepository repository : repositoryList) {
                if (repositories.put(repository.providerName(), repository) != null) {
                    throw new IllegalArgumentException("duplicate idempotency repository: " + repository.providerName());
                }
            }
        }

        defaults.put(IdempotencyMode.WINDOWED, windowedDefault);
        defaults.put(IdempotencyMode.DURABLE, durableDefault);
    }

    /**
     * 按 Policy 解析 Repository，并在业务真正开始前校验 Provider capability。
     */
    @Override
    public IdempotencyRepository resolve(IdempotencyMode mode, String requestedName) {
        IdempotencyMode resolvedMode = mode == null ? IdempotencyMode.DURABLE : mode;
        String name = requestedName == null || requestedName.isBlank() ? defaults.get(resolvedMode) : requestedName;

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("no default repository for mode: " + resolvedMode);
        }

        IdempotencyRepository repository = repositories.get(name);
        if (repository == null) {
            throw new IllegalArgumentException("idempotency repository not found: " + name);
        }
        if (!repository.capabilities().supports(resolvedMode)) {
            throw new IllegalArgumentException("repository " + name + " does not support " + resolvedMode);
        }
        return repository;
    }
}
