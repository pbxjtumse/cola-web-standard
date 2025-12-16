package com.xjtu.iron.cola.web;

public interface MessageProducer {
    SendResult send(Message<?> message, SendContext context);
}
