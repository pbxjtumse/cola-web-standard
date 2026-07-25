package com.xjtu.iron.message.api.spi;

/**
 * 表示 Provider 对公共消息能力的支持情况。
 */
public enum MessageCapability {

    /** 支持普通消息发布。 */
    BASIC_PUBLISH,

    /** 支持普通消息订阅消费。 */
    BASIC_CONSUME
}
