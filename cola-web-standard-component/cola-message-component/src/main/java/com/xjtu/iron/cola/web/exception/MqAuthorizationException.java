package com.xjtu.iron.cola.web.exception;

/**
 * 适用场景：
 *  Kafka：AuthorizationException / TopicAuthorizationException
 *  RocketMQ：无权限
 *  RabbitMQ：vhost / exchange 权限问题
 * 特点：
 *  100% BROKER_REJECT
 *  重试无意义
 *  必须人工介入
 * @author pangbo
 * @date 2025/12/17
 */
public class MqAuthorizationException extends MqException {
    /**
     * @param cause
     */
    public MqAuthorizationException(Throwable cause) {
        super("Authorization failed", cause);
    }
}

