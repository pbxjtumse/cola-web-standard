package com.xjtu.iron.message.api;

import java.util.concurrent.CompletionStage;

/**
 * 定义业务侧统一的消息发布入口。
 */
public interface MessagePublisher {

    /**
     * 同步发送消息并等待确认结果。
     *
     * @param destination 逻辑消息目的地
     * @param message 业务消息信封
     * @param options 本次发送选项
     * @return 标准发送结果
     */
    SendResult send(
            MessageDestination destination,
            MessageEnvelope<?> message,
            SendOptions options);

    /**
     * 异步发送消息并异步获得确认结果。
     *
     * @param destination 逻辑消息目的地
     * @param message 业务消息信封
     * @param options 本次发送选项
     * @return 标准发送结果的异步阶段
     */
    CompletionStage<SendResult> sendAsync(
            MessageDestination destination,
            MessageEnvelope<?> message,
            SendOptions options);
}
