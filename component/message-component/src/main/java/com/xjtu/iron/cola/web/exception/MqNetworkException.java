package com.xjtu.iron.cola.web.exception;

/**
 * 适用场景：
 *  Kafka：NetworkException
 *  RocketMQ：RemotingException
 *  RabbitMQ：Connection reset / channel closed
 * 特点：
 *  通常 UNCERTAIN
 *  有时可重试
 *  必须保留原始异常
 * @author pangbo
 * @date 2025/12/17
 */
public class MqNetworkException extends MqException {
    /**
     * @param cause
     */
    public MqNetworkException(Throwable cause) {
        super("Network error while sending message", cause);
    }
}

