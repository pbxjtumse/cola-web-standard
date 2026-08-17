package com.xjtu.iron.idempotent.core.support;

import com.xjtu.iron.idempotent.core.policy.IdempotencyPolicyRegistry;

import com.xjtu.iron.idempotent.api.policy.IdempotencyMode;
import com.xjtu.iron.idempotent.api.policy.IdempotencyOptions;

import java.util.Objects;

/**
 * V1.2 兼容默认策略容器。
 *
 * @deprecated V1.3 使用 {@link IdempotencyPolicyRegistry}。
 */
@Deprecated
public final class IdempotencyDefaults {

    private final IdempotencyMode defaultMode;
    private final IdempotencyOptions windowed;
    private final IdempotencyOptions durable;

    public IdempotencyDefaults(
            IdempotencyMode defaultMode,
            IdempotencyOptions windowed,
            IdempotencyOptions durable) {
        this.defaultMode = Objects.requireNonNull(defaultMode, "defaultMode must not be null");
        this.windowed = Objects.requireNonNull(windowed, "windowed must not be null");
        this.durable = Objects.requireNonNull(durable, "durable must not be null");
    }

    public IdempotencyOptions defaultOptions() {
        return defaultMode.isWindowed() ? windowed : durable;
    }
}
