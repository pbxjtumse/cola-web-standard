package com.xjtu.iron.cola.web.exception;

/**
 * 适用场景：
 *  Kafka：SerializationException
 *  RocketMQ：编码失败
 *  RabbitMQ：MessageConverter 异常
 * 特点：
 *  100% CLIENT_FAIL
 *  永远不该重试
 *  直接进告警 / 死信 / 修代码
 * @author pangbo
 * @date 2025/12/17
 */
public class MqSerializationException extends MqException {
    /**
     * @param cause
     */
    public MqSerializationException(Throwable cause) {
        super("Message serialization failed", cause);
    }
}

