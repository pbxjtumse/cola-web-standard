package com.xjtu.iron.transaction.spi;

import com.xjtu.iron.transaction.api.TransactionOutcome;
import com.xjtu.iron.transaction.api.TransactionStage;

/**
 * Provider 到 core 之间的基础设施异常。
 * core 会把它转换成稳定的 API 异常 TransactionExecutionException。
 */
public class ProviderTransactionException extends RuntimeException {

    private final TransactionStage stage;
    private final TransactionOutcome outcome;

    public ProviderTransactionException(
            String message,
            TransactionStage stage,
            TransactionOutcome outcome,
            Throwable cause) {
        super(message, cause);
        this.stage = stage;
        this.outcome = outcome;
    }

    public TransactionStage stage() { return stage; }
    public TransactionOutcome outcome() { return outcome; }
}
