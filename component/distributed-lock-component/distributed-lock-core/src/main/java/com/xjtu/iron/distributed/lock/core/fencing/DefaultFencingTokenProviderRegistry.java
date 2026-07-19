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
 */
public final class DefaultFencingTokenProviderRegistry implements FencingTokenProviderRegistry {

    private final Map<String, FencingTokenProvider> providers;
    private final String defaultProviderName;

    public DefaultFencingTokenProviderRegistry(Collection<? extends FencingTokenProvider> providers) {
        this(null, providers);
    }

    public DefaultFencingTokenProviderRegistry(
            String defaultProviderName,
            Collection<? extends FencingTokenProvider> providers
    ) {
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
        this.defaultProviderName = normalizeNullable(defaultProviderName);
        if (this.defaultProviderName != null && !mapped.containsKey(this.defaultProviderName)) {
            throw new IllegalArgumentException(
                    "default fencing token provider not found: " + this.defaultProviderName);
        }
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
    public Optional<FencingTokenProvider> defaultProvider() {
        if (defaultProviderName != null) {
            return Optional.ofNullable(providers.get(defaultProviderName));
        }
        /* 只有一个独立 Provider 时可以安全推导默认值；多个 Provider 时禁止猜测。 */
        if (providers.size() == 1) {
            return providers.values().stream().findFirst();
        }
        return Optional.empty();
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
