package com.xjtu.iron.message.core.consume.idempotency;

import com.xjtu.iron.message.api.consume.ConsumeContext;
import com.xjtu.iron.message.api.consume.MessageIdempotencyOptions;
import com.xjtu.iron.message.api.model.MessageEnvelope;

/**
 * 解析消费幂等 scene。
 */
public interface MessageIdempotencySceneResolver {
    String resolve(MessageEnvelope<?> message, ConsumeContext context, MessageIdempotencyOptions options);
}
