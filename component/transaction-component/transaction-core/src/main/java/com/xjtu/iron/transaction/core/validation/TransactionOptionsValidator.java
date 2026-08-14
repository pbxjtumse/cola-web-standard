package com.xjtu.iron.transaction.core.validation;

import com.xjtu.iron.transaction.api.definition.TransactionOptions;

import java.time.Duration;
import java.util.Objects;

/**
 * 一期事务选项校验器。
 */
public final class TransactionOptionsValidator {

    private TransactionOptionsValidator() {
    }

    public static void validate(TransactionOptions options) {
        // 在进入 Provider 之前完成稳定 API 层校验，避免把明显非法参数推给具体事务框架。
        Objects.requireNonNull(options, "options");

        if (options.name() == null || options.name().isBlank()) {
            throw new IllegalArgumentException("transaction name must not be blank");
        }

        Objects.requireNonNull(options.propagation(), "transaction propagation");
        Objects.requireNonNull(options.isolation(), "transaction isolation");

        Duration timeout = options.timeout();
        if (timeout != null && (timeout.isZero() || timeout.isNegative())) {
            throw new IllegalArgumentException("transaction timeout must be positive");
        }
    }
}
