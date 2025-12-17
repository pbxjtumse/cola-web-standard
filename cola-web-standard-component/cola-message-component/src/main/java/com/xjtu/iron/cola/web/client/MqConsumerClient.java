package com.xjtu.iron.cola.web.client;

import com.xjtu.iron.cola.web.handler.RawMessageHandler;

public interface MqConsumerClient {
    void subscribe(Subscription subscription, RawMessageHandler handler);
}
