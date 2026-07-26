package com.xjtu.iron.message.api;

import java.util.concurrent.CompletionStage;

/**
 * 面向业务代码的统一普通消息发布接口。
 */
public interface MessagePublisher {

    /**
     * 同步发送普通消息并等待标准发送结果。
     *
     * @param destination 逻辑目的地
     * @param message 消息信封
     * @param options 发送选项
     * @return 标准发送结果
     */
    SendResult send(
            MessageDestination destination,
            MessageEnvelope<?> message,
            SendOptions options);

    /**
     * 异步发送普通消息。
     *
     * @param destination 逻辑目的地
     * @param message 消息信封
     * @param options 发送选项
     * @return 最终完成为标准发送结果的异步阶段
     */
    CompletionStage<SendResult> sendAsync(
            MessageDestination destination,
            MessageEnvelope<?> message,
            SendOptions options);
}
