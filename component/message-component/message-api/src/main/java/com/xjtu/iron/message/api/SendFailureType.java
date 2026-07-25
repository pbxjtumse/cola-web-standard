package com.xjtu.iron.message.api;

/**
 * 表示与具体中间件无关的发送失败分类。
 */
public enum SendFailureType {

    /** 当前发送没有失败。 */
    NONE,

    /** 发送参数不合法。 */
    VALIDATION_ERROR,

    /** 消息序列化失败。 */
    SERIALIZATION_ERROR,

    /** 当前 Provider 不支持业务要求的能力。 */
    UNSUPPORTED_CAPABILITY,

    /** 无法找到或初始化指定 Provider。 */
    PROVIDER_ERROR,

    /** Broker 明确拒绝本次发送。 */
    BROKER_REJECTED,

    /** 认证或鉴权失败。 */
    AUTHENTICATION_ERROR,

    /** 网络通信失败。 */
    NETWORK_ERROR,

    /** 请求或确认等待超时。 */
    TIMEOUT,

    /** Provider 客户端内部执行失败。 */
    CLIENT_ERROR,

    /** 当前无法准确归类的异常。 */
    UNKNOWN
}
