package com.xjtu.iron.message.core.consume.idempotency;

import com.xjtu.iron.message.api.consume.ConsumeContext;
import com.xjtu.iron.message.api.consume.ConsumeDecision;
import com.xjtu.iron.message.api.consume.MessageIdempotencyOptions;
import com.xjtu.iron.message.api.model.MessageEnvelope;
import com.xjtu.iron.message.core.consume.ConsumeInvocation;

/**
 * 消息消费幂等执行器。
 */
public interface MessageIdempotencyExecutor {
    ConsumeDecision execute(MessageEnvelope<?> message, ConsumeContext context, MessageIdempotencyOptions options, ConsumeInvocation invocation);
}
