package com.xjtu.iron.message.client;

import com.xjtu.iron.message.Message;


/**
 * @author pangbo
 * @date 2025/12/18
 */
public interface MqProducerClient {

    /**
     * 同步发送
     * 成功：不返回
     * 失败：抛 MqException 子类
     * @param message
     */
    void send(Message<?> message);

    /**
     * 异步发送
     * @param message
     * @param callback
     */
    void sendAsync(Message<?> message, MqSendCallback callback);
}
