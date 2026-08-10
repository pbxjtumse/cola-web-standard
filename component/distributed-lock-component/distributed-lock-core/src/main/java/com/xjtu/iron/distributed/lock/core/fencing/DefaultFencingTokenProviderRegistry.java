package com.xjtu.iron.distributed.lock.core.fencing;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 默认 fencing token Provider 注册表。
 *
 * <p>只维护 providerName -> Provider 的稳定映射。这里刻意不设计 defaultProvider：
 * external fencing 的选择必须由 {@link FencingTokenCoordinator} 根据显式配置完成，
 * 避免容器中 Bean 数量变化悄悄改变一致性策略。</p>
 */
public final class DefaultFencingTokenProviderRegistry implements FencingTokenProviderRegistry {

    private final Map<String, FencingTokenProvider> providers;

    public DefaultFencingTokenProviderRegistry(Collection<? extends FencingTokenProvider> providers) {
        Map<String, FencingTokenProvider> mapped = new LinkedHashMap<>();
        if (providers != null) {
            for (FencingTokenProvider provider : providers) {
                Objects.requireNonNull(provider, "fencing token provider must not be null");
                String name = normalize(provider.providerName());
                FencingTokenProvider previous = mapped.putIfAbsent(name, provider);
                if (previous != null) {
                    throw new IllegalArgumentException("duplicate fencing token provider: " + name);
                }
            }
        }
        this.providers = Collections.unmodifiableMap(mapped);
    }

    @Override
    public Optional<FencingTokenProvider> findProvider(String providerName) {
        String normalized = normalizeNullable(providerName);
        if (normalized == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(providers.get(normalized));
    }

    @Override
    public Set<String> providerNames() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(providers.keySet()));
    }

    private static String normalize(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new IllegalArgumentException("providerName must not be blank");
        }
        return normalized;
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
