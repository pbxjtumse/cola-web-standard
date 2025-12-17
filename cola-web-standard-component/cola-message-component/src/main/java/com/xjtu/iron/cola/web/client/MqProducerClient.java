package com.xjtu.iron.cola.web.client;

import com.xjtu.iron.cola.web.Message;
import com.xjtu.iron.cola.web.result.SendResult;

public interface MqProducerClient {

    void send(Message<?> message);
    void sendAsync(Message<?> message, MqSendCallback callback);
}
