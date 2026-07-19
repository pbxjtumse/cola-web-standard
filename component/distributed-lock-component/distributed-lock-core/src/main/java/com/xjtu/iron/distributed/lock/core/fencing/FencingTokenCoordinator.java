package com.xjtu.iron.distributed.lock.core.fencing;

import com.xjtu.iron.distributed.lock.api.LockOptions;
import com.xjtu.iron.distributed.lock.core.spi.LockProvider;
import com.xjtu.iron.distributed.lock.core.spi.model.LockLease;

import java.util.Objects;

/**
 * fencing token 计划选择与独立发号协调器。
 *
 * <p>优先级：</p>
 * <ol>
 *     <li>未要求 fencing：NONE；</li>
 *     <li>LockOptions 显式指定当前锁 Provider 名称：NATIVE；</li>
 *     <li>LockOptions 显式指定独立 Provider：EXTERNAL；</li>
 *     <li>未显式指定且锁 Provider 支持原生 fencing：NATIVE；</li>
 *     <li>注册表存在唯一或默认独立 Provider：EXTERNAL；</li>
 *     <li>否则参数校验失败。</li>
 * </ol>
 */
public final class FencingTokenCoordinator {

    private final FencingTokenProviderRegistry registry;

    public FencingTokenCoordinator(FencingTokenProviderRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
    }

    public FencingTokenPlan plan(LockProvider lockProvider, LockOptions options) {
        Objects.requireNonNull(lockProvider, "lockProvider must not be null");
        Objects.requireNonNull(options, "options must not be null");
        if (!options.isFencingRequired()) {
            return FencingTokenPlan.none();
        }

        String explicitProviderName = trimToNull(options.getFencingTokenProviderName());
        if (explicitProviderName != null) {
            /*
             * 显式名称既可以指向当前 LockProvider 的原生 fencing（例如 redis），
             * 也可以指向独立 FencingTokenProvider（例如 jdbc-sequence）。
             */
            if (explicitProviderName.equals(lockProvider.providerName())) {
                if (!lockProvider.capabilities().isFencingTokenSupported()) {
                    throw new IllegalArgumentException(
                            "lock provider does not support native fencing token: "
                                    + lockProvider.providerName());
                }
                return FencingTokenPlan.nativeProvider();
            }
            return FencingTokenPlan.external(registry.getRequired(explicitProviderName));
        }

        if (lockProvider.capabilities().isFencingTokenSupported()) {
            return FencingTokenPlan.nativeProvider();
        }

        return registry.defaultProvider()
                .map(FencingTokenPlan::external)
                .orElseThrow(() -> new IllegalArgumentException(
                        "fencing token is required, but lock provider has no native support "
                                + "and no default external provider is configured: "
                                + lockProvider.providerName()));
    }

    public FencingTokenResponse issueExternal(
            FencingTokenPlan plan,
            LockLease lease,
            LockOptions options
    ) {
        if (!plan.isExternal()) {
            throw new IllegalArgumentException("plan must be EXTERNAL");
        }
        FencingTokenProvider provider = plan.externalProvider().orElseThrow();
        FencingTokenRequest request = FencingTokenRequest.builder()
                .namespace(lease.getNamespace())
                .lockName(lease.getLockName())
                .ownerToken(lease.getOwnerToken())
                .options(options)
                .build();
        if (!provider.supports(request)) {
            return FencingTokenResponse.notSupported(
                    "fencing token provider does not support request: " + provider.providerName());
        }
        try {
            FencingTokenResponse response = provider.nextToken(request);
            if (response == null) {
                return FencingTokenResponse.failed(new IllegalStateException(
                        "fencing token provider returned null response: " + provider.providerName()));
            }
            return response;
        } catch (Throwable error) {
            return FencingTokenResponse.failed(error);
        }
    }

    private static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
