package com.xjtu.iron.cola.web.exception;

/**
 * MQ 抽象层的根异常
 * 所有中间件异常最终必须被映射为它的子类
 *
 * @author pangbo
 * @date 2025/12/17
 */
public abstract class MqException extends RuntimeException {
    /**
     * @param message
     * @param cause
     */
    protected MqException(String message, Throwable cause) {
        super(message, cause);
    }
}
