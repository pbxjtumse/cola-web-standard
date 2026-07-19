package com.xjtu.iron.distributed.lock.core.fencing;

import java.util.Objects;
import java.util.Optional;

/**
 * 一次加锁请求选定的 fencing token 生成计划。
 */
public final class FencingTokenPlan {

    private static final FencingTokenPlan NONE = new FencingTokenPlan(FencingTokenMode.NONE, null);
    private static final FencingTokenPlan NATIVE = new FencingTokenPlan(FencingTokenMode.NATIVE, null);

    private final FencingTokenMode mode;
    private final FencingTokenProvider externalProvider;

    private FencingTokenPlan(FencingTokenMode mode, FencingTokenProvider externalProvider) {
        this.mode = Objects.requireNonNull(mode, "mode must not be null");
        this.externalProvider = externalProvider;
        if (mode == FencingTokenMode.EXTERNAL && externalProvider == null) {
            throw new IllegalArgumentException("externalProvider must not be null for EXTERNAL mode");
        }
        if (mode != FencingTokenMode.EXTERNAL && externalProvider != null) {
            throw new IllegalArgumentException("externalProvider must be null unless mode is EXTERNAL");
        }
    }

    public static FencingTokenPlan none() {
        return NONE;
    }

    public static FencingTokenPlan nativeProvider() {
        return NATIVE;
    }

    public static FencingTokenPlan external(FencingTokenProvider provider) {
        return new FencingTokenPlan(FencingTokenMode.EXTERNAL,
                Objects.requireNonNull(provider, "provider must not be null"));
    }

    public FencingTokenMode mode() {
        return mode;
    }

    public boolean isRequired() {
        return mode != FencingTokenMode.NONE;
    }

    public boolean isNative() {
        return mode == FencingTokenMode.NATIVE;
    }

    public boolean isExternal() {
        return mode == FencingTokenMode.EXTERNAL;
    }

    public Optional<FencingTokenProvider> externalProvider() {
        return Optional.ofNullable(externalProvider);
    }

    public String sourceName(String lockProviderName) {
        if (mode == FencingTokenMode.NATIVE) {
            return lockProviderName;
        }
        if (mode == FencingTokenMode.EXTERNAL) {
            return externalProvider.providerName();
        }
        return null;
    }
}
