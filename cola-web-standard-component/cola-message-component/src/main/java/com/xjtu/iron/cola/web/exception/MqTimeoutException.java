package com.xjtu.iron.cola.web.exception;

/**
 * 适用场景：
 *  Kafka：TimeoutException
 *  RocketMQ：发送超时
 *  RabbitMQ：confirm 超时
 * 特点：
 *  UNCERTAIN
 *  最危险
 *  通常需要 Outbox / 幂等 / 人工兜底
 * @author pangbo
 * @date 2025/12/17
 */
public class MqTimeoutException extends MqException {
    /**
     * @param cause
     */
    public MqTimeoutException(Throwable cause) {
        super("Send timeout", cause);
    }
}

