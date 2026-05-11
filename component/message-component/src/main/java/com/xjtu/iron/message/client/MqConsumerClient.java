package com.xjtu.iron.message.client;

import com.xjtu.iron.message.handler.RawMessageHandler;

public interface MqConsumerClient {
    void subscribe(Subscription subscription, RawMessageHandler handler);
}
