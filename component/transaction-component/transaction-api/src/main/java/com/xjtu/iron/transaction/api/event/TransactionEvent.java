package com.xjtu.iron.transaction.api.event;

import com.xjtu.iron.transaction.api.context.TransactionParticipation;
import com.xjtu.iron.transaction.api.status.TransactionOutcome;
import com.xjtu.iron.transaction.api.status.TransactionStage;

import java.time.Duration;

/**
 * 事务生命周期事件。
 *
 * <p>事件只承载事务元信息，不默认承载 SQL、业务请求体、密码或 Token。</p>
 */
public final class TransactionEvent {

    private final TransactionEventType type;
    private final String executionId;
    private final String transactionName;
    private final TransactionStage stage;
    private final TransactionParticipation participation;
    private final TransactionOutcome outcome;
    private final Duration elapsed;
    private final Throwable failure;

    public TransactionEvent(
            TransactionEventType type,
            String executionId,
            String transactionName,
            TransactionStage stage,
            TransactionParticipation participation,
            TransactionOutcome outcome,
            Duration elapsed,
            Throwable failure) {
        this.type = type;
        this.executionId = executionId;
        this.transactionName = transactionName;
        this.stage = stage;
        this.participation = participation;
        this.outcome = outcome;
        this.elapsed = elapsed;
        this.failure = failure;
    }

    public TransactionEventType type() { return type; }
    public String executionId() { return executionId; }
    public String transactionName() { return transactionName; }
    public TransactionStage stage() { return stage; }
    public TransactionParticipation participation() { return participation; }
    public TransactionOutcome outcome() { return outcome; }
    public Duration elapsed() { return elapsed; }
    public Throwable failure() { return failure; }
}
