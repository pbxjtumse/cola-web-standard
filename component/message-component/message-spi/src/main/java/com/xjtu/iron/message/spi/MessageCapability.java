package com.xjtu.iron.message.spi;

/**
 * 描述 Provider 对公共消息能力的支持情况。
 *
 * <p>一期只声明普通发布和普通消费。事务、顺序、延时和回放将在三期通过
 * 专属能力接口表达，而不是提前塞入一个巨大公共枚举。</p>
 */
public enum MessageCapability {

    /** 支持普通消息发布。 */
    BASIC_PUBLISH,

    /** 支持普通消息消费。 */
    BASIC_CONSUME
}
