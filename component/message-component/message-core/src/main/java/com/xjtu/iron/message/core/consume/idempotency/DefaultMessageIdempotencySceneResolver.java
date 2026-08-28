package com.xjtu.iron.message.core.consume.idempotency;

import com.xjtu.iron.message.api.consume.ConsumeContext;
import com.xjtu.iron.message.api.consume.MessageIdempotencyOptions;
import com.xjtu.iron.message.api.model.MessageEnvelope;

/**
 * 默认 scene 解析：优先使用配置 scene，否则使用 consumerGroup。
 */
public final class DefaultMessageIdempotencySceneResolver implements MessageIdempotencySceneResolver {
    @Override
    public String resolve(MessageEnvelope<?> message, ConsumeContext context, MessageIdempotencyOptions options) {
        if (options.scene() != null && !options.scene().isBlank()) {
            return options.scene().trim();
        }
        if (context.consumerGroup() == null || context.consumerGroup().isBlank()) {
            throw new IllegalArgumentException("consumerGroup must not be blank when resolving idempotency scene");
        }
        return context.consumerGroup();
    }
}
