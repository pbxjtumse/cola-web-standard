package com.xjtu.iron.cola.web;

import com.xjtu.iron.cola.web.result.SendResult;

public interface MessageProducer {
    SendResult send(Message<?> message, SendContext context);
}
