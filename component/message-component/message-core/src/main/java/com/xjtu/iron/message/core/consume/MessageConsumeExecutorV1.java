package com.xjtu.iron.message.core.consume;

import com.xjtu.iron.message.api.consume.context.ConsumeContext;
import com.xjtu.iron.message.api.consume.decision.ConsumeDecision;
import com.xjtu.iron.message.api.consume.handler.MessageHandler;
import com.xjtu.iron.message.api.model.MessageEnvelope;
import com.xjtu.iron.message.core.consume.strategy.IdempotencyStrategy;
import com.xjtu.iron.message.core.consume.strategy.TransactionStrategy;

import java.util.Objects;

/**
 * 消费执行模板 V1。
 *
 * <p>固定消费顺序：幂等策略 -> 事务策略 -> 业务 Handler。</p>
 */
public final class MessageConsumeExecutorV1 {

    private final IdempotencyStrategy idempotencyStrategy;
    private final TransactionStrategy transactionStrategy;
    private final ConsumeExceptionClassifier exceptionClassifier;

    public MessageConsumeExecutorV1(
            IdempotencyStrategy idempotencyStrategy,
            TransactionStrategy transactionStrategy,
            ConsumeExceptionClassifier exceptionClassifier) {
        this.idempotencyStrategy = Objects.requireNonNull(idempotencyStrategy);
        this.transactionStrategy = Objects.requireNonNull(transactionStrategy);
        this.exceptionClassifier = Objects.requireNonNull(exceptionClassifier);
    }

    public <T> ConsumeDecision execute(
            MessageEnvelope<T> message,
            ConsumeContext context,
            MessageHandler<T> handler) {
        return idempotencyStrategy.execute(
                message,
                context,
                () -> transactionStrategy.execute(
                        () -> invokeHandler(message, context, handler)));
    }

    private <T> ConsumeDecision invokeHandler(
            MessageEnvelope<T> message,
            ConsumeContext context,
            MessageHandler<T> handler) {
        try {
            ConsumeDecision decision = handler.handle(message, context);
            return decision == null ? ConsumeDecision.RETRY : decision;
        } catch (RuntimeException exception) {
            return exceptionClassifier.classify(exception, context);
        }
    }
}
