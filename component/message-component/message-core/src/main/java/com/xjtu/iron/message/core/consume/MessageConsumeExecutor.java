package com.xjtu.iron.message.core.consume;

import com.xjtu.iron.message.api.consume.context.ConsumeContext;
import com.xjtu.iron.message.api.consume.decision.ConsumeDecision;
import com.xjtu.iron.message.api.consume.handler.MessageHandler;
import com.xjtu.iron.message.api.model.MessageEnvelope;
import com.xjtu.iron.message.core.consume.strategy.IdempotencyStrategy;
import com.xjtu.iron.message.core.consume.strategy.TransactionStrategy;
import com.xjtu.iron.message.core.consume.handler.MessageHandlerInvoker;

import java.util.Objects;

/**
 * 消费执行模板 V1。
 *
 * <p>固定消费顺序：幂等策略 -> 事务策略 -> 业务 Handler。</p>
 */
public final class MessageConsumeExecutor {

    private final IdempotencyStrategy idempotencyStrategy;
    private final TransactionStrategy transactionStrategy;
    private final MessageHandlerInvoker handlerInvoker;

    public MessageConsumeExecutor(
            IdempotencyStrategy idempotencyStrategy,
            TransactionStrategy transactionStrategy,
            MessageHandlerInvoker handlerInvoker) {
        this.idempotencyStrategy = Objects.requireNonNull(idempotencyStrategy);
        this.transactionStrategy = Objects.requireNonNull(transactionStrategy);
        this.handlerInvoker = Objects.requireNonNull(handlerInvoker);
    }

    public <T> ConsumeDecision execute(
            MessageEnvelope<T> message,
            ConsumeContext context,
            MessageHandler<T> handler) {
        return idempotencyStrategy.execute(message, context,
                () -> transactionStrategy.execute(() -> handlerInvoker.invoke(message, context, handler)));
    }
}
