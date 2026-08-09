package com.xjtu.iron.message.core.send;

import com.xjtu.iron.message.api.SendResult;

import java.util.concurrent.CompletionStage;

/**
 * message-core 内部使用的发送执行器。
 */
public interface MessageSendExecutor {

    /**
     * 同步执行一次消息发送。
     *
     * @param prepared 已准备好的发送快照
     * @return 统一发送结果
     */
    SendResult send(PreparedMessageSend prepared);

    /**
     * 异步执行一次消息发送。
     *
     * @param prepared 已准备好的发送快照
     * @return 异步发送结果
     */
    CompletionStage<SendResult> sendAsync(PreparedMessageSend prepared);
}
