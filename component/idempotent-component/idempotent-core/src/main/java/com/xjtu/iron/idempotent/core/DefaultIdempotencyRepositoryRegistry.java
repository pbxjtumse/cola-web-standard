package com.xjtu.iron.idempotent.core;

import com.xjtu.iron.idempotent.api.IdempotencyMode;
import com.xjtu.iron.idempotent.api.repository.IdempotencyRepository;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认 Repository 注册表。
 *
 * <p>它解决两个问题：</p>
 * <ol>
 *     <li>把所有 {@link IdempotencyRepository} 按 {@code providerName()} 注册；</li>
 *     <li>为 SHORT_TERM / DURABLE 分别保存默认 Provider 名称。</li>
 * </ol>
 *
 * <p>Registry 只负责选择实现，不负责状态机。真正的原子性仍由具体 Repository 保证。</p>
 */
public final class DefaultIdempotencyRepositoryRegistry
        implements IdempotencyRepositoryRegistry {

    private final Map<String, IdempotencyRepository> repositories = new LinkedHashMap<>();
    private final Map<IdempotencyMode, String> defaults = new EnumMap<>(IdempotencyMode.class);

    public DefaultIdempotencyRepositoryRegistry(
            List<IdempotencyRepository> repositoryList,
            String shortTermDefault,
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

        defaults.put(IdempotencyMode.SHORT_TERM, shortTermDefault);
        defaults.put(IdempotencyMode.DURABLE, durableDefault);
    }

    @Override
    public IdempotencyRepository resolve(IdempotencyMode mode, String requestedName) {
        String name = requestedName == null || requestedName.isBlank()
                ? defaults.get(mode)
                : requestedName;

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("no default repository for mode: " + mode);
        }

        IdempotencyRepository repository = repositories.get(name);
        if (repository == null) {
            throw new IllegalArgumentException("idempotency repository not found: " + name);
        }

        // 防止误配置：例如 DURABLE 不能被当前仅支持 SHORT_TERM 的 Redis Repository 接管。
        if (!repository.supports(mode)) {
            throw new IllegalArgumentException(
                    "repository " + name + " does not support " + mode);
        }
        return repository;
    }
}
