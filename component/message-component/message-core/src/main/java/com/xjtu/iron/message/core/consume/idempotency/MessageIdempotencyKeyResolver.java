package com.xjtu.iron.message.core.consume.idempotency;

import com.xjtu.iron.message.api.consume.context.ConsumeContext;
import com.xjtu.iron.message.api.consume.definition.MessageIdempotencyOptions;
import com.xjtu.iron.message.api.model.MessageEnvelope;

/**
 * 解析消费幂等 key。
 */
public interface MessageIdempotencyKeyResolver {
    String resolve(MessageEnvelope<?> message, ConsumeContext context, MessageIdempotencyOptions options);
}
