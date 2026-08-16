package com.xjtu.iron.idempotent.core.transaction;

import java.util.Objects;

/**
 * transaction-component 基础设施异常在幂等 Core 内的稳定表示。
 *
 * <p>它只表示 BEGIN/COMMIT/ROLLBACK 等事务基础设施问题，不用于包装业务 callback 自己抛出的异常。</p>
 */
public final class IdempotencyTransactionException extends RuntimeException {

    private final String stage;
    private final IdempotencyTransactionOutcome outcome;

    public IdempotencyTransactionException(
            String message,
            String stage,
            IdempotencyTransactionOutcome outcome,
            Throwable cause) {
        super(message, cause);
        this.stage = stage == null ? "UNKNOWN" : stage;
        this.outcome = Objects.requireNonNull(outcome, "outcome must not be null");
    }

    public String stage() {
        return stage;
    }

    public IdempotencyTransactionOutcome outcome() {
        return outcome;
    }
}
