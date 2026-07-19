package com.xjtu.iron.distributed.lock.core.fencing;

import java.util.Optional;
import java.util.Set;

/**
 * 独立 fencing token Provider 注册表。
 */
public interface FencingTokenProviderRegistry {

    Optional<FencingTokenProvider> findProvider(String providerName);

    Optional<FencingTokenProvider> defaultProvider();

    default FencingTokenProvider getRequired(String providerName) {
        return findProvider(providerName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "fencing token provider not found: " + providerName));
    }

    Set<String> providerNames();
}
