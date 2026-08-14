package com.xjtu.iron.transaction.spi.provider;

import com.xjtu.iron.transaction.api.context.TransactionParticipation;
import com.xjtu.iron.transaction.api.status.TransactionOutcome;

import java.util.Objects;

/**
 * Provider 成功完成一次事务逻辑后的结果。
 */
public final class TransactionProviderResult<T> {

    private final T value;
    private final TransactionParticipation participation;
    private final TransactionOutcome outcome;

    public TransactionProviderResult(
            T value,
            TransactionParticipation participation,
            TransactionOutcome outcome) {
        this.value = value;
        this.participation = Objects.requireNonNull(participation, "participation");
        this.outcome = Objects.requireNonNull(outcome, "outcome");
    }

    public T value() { return value; }
    public TransactionParticipation participation() { return participation; }
    public TransactionOutcome outcome() { return outcome; }
}
