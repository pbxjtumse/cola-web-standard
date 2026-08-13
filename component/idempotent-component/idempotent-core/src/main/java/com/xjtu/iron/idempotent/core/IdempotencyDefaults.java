package com.xjtu.iron.idempotent.core;

import com.xjtu.iron.idempotent.api.IdempotencyMode;
import com.xjtu.iron.idempotent.api.IdempotencyOptions;

import java.util.Objects;

/**
 * Starter 解析配置后交给 Core 的默认策略快照。
 *
 * <p>Core 不读取 Spring Environment，也不依赖 {@code @ConfigurationProperties}。
 * Starter 负责把外部配置转换成两个不可变的 {@link IdempotencyOptions}，
 * Core 只消费这里的纯 Java 对象。</p>
 */
public final class IdempotencyDefaults {

    private final IdempotencyMode defaultMode;
    private final IdempotencyOptions shortTerm;
    private final IdempotencyOptions durable;

    public IdempotencyDefaults(
            IdempotencyMode defaultMode,
            IdempotencyOptions shortTerm,
            IdempotencyOptions durable) {
        this.defaultMode = Objects.requireNonNull(defaultMode, "defaultMode must not be null");
        this.shortTerm = Objects.requireNonNull(shortTerm, "shortTerm must not be null");
        this.durable = Objects.requireNonNull(durable, "durable must not be null");
    }

    /**
     * 当一次请求没有显式传入 Options 时，返回组件级默认模式对应的配置快照。
     */
    public IdempotencyOptions defaultOptions() {
        return defaultMode == IdempotencyMode.SHORT_TERM ? shortTerm : durable;
    }
}
