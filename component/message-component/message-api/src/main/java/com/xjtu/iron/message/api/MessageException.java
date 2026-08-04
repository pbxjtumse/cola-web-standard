package com.xjtu.iron.message.api;

/**
 * 消息组件基础运行时异常。
 */
public class MessageException extends RuntimeException {

    /** 序列化版本号，避免开启编译告警时产生 serial 警告。 */
    private static final long serialVersionUID = 1L;

    public MessageException(String message) {
        super(message);
    }

    public MessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
