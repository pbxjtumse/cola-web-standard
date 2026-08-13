package com.xjtu.iron.transaction.core;

import com.xjtu.iron.transaction.api.TransactionOptions;

import java.time.Duration;
import java.util.Objects;

/**
 * 一期事务选项校验。
 */
final class TransactionOptionsValidator {

    private TransactionOptionsValidator() {
    }

    static void validate(TransactionOptions options) {
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
