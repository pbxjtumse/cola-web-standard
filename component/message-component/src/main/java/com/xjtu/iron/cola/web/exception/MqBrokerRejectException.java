package com.xjtu.iron.cola.web.exception;

/**
 * 适用场景：
 * Topic 不存在
 * 配额超限
 * 消息过大
 * RocketMQ：NO_PERMISSION / MESSAGE_ILLEGAL
 *特点：
 * FAIL + BROKER_REJECT
 * 重试通常无意义
 * 非代码 bug，而是配置 / 运维问题
 * @author pangbo
 * @date 2025/12/17
 */
public class MqBrokerRejectException extends MqException {
    /**
     * @param reason
     * @param cause
     */
    public MqBrokerRejectException(String reason, Throwable cause) {
        super(reason, cause);
    }
}

