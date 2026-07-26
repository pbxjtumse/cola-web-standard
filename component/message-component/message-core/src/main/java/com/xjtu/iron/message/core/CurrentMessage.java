package com.xjtu.iron.message.core;

import com.xjtu.iron.message.api.ConsumeContext;
import com.xjtu.iron.message.api.MessageEnvelope;

import java.util.Objects;

/**
 * 表示当前线程正在处理的入站消息。
 *
 * @param envelope 当前入站消息信封
 * @param consumeContext 当前投递运行时上下文
 */
public record CurrentMessage(
        MessageEnvelope<?> envelope,
        ConsumeContext consumeContext) {

    /**
     * 校验当前消息上下文。
     */
    public CurrentMessage {
        // 入站消息信封不能为空。
        envelope = Objects.requireNonNull(envelope, "envelope must not be null");
        // 消费上下文不能为空。
        consumeContext = Objects.requireNonNull(consumeContext, "consumeContext must not be null");
    }
}
