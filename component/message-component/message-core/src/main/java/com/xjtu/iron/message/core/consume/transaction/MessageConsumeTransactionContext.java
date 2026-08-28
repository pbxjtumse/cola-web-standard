package com.xjtu.iron.message.core.consume.transaction;

import com.xjtu.iron.message.api.consume.context.ConsumeContext;
import com.xjtu.iron.message.core.consume.idempotency.MessageIdempotencyContext;

/**
 * 消费事务上下文。
 */
public final class MessageConsumeTransactionContext {
    private final ConsumeContext consumeContext;
    private final MessageIdempotencyContext idempotencyContext;

    public MessageConsumeTransactionContext(ConsumeContext consumeContext, MessageIdempotencyContext idempotencyContext) {
        this.consumeContext = consumeContext;
        this.idempotencyContext = idempotencyContext;
    }

    public ConsumeContext consumeContext() { return consumeContext; }
    public MessageIdempotencyContext idempotencyContext() { return idempotencyContext; }
}
