package com.xjtu.iron.cola.web.exception;

/**
 *适用场景：
 *  你没想到的
 *  中间件升级后新增的
 *  第三方 SDK 的 bug
 *特点：
 *  绝不能丢
 *  默认映射为 UNCERTAIN
 *  原始异常必须完整保留
 * @author pangbo
 * @date 2025/12/17
 */
public class MqUnknownException extends MqException {
    /**
     * @param cause
     */
    public MqUnknownException(Throwable cause) {
        super("Unknown MQ exception", cause);
    }
}

