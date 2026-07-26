package com.xjtu.iron.message.api;

/**
 * 描述发送未确认成功时的标准失败原因。
 */
public enum SendFailureType {

    /** 没有失败。 */
    NONE,

    /** 公共消息参数非法。 */
    VALIDATION_ERROR,

    /** 业务消息体序列化失败。 */
    SERIALIZATION_ERROR,

    /** 逻辑目的地无法解析为物理目的地。 */
    ROUTING_ERROR,

    /** 指定 Provider 没有注册。 */
    PROVIDER_NOT_FOUND,

    /** 当前 Provider 不支持所需能力。 */
    UNSUPPORTED_CAPABILITY,

    /** 身份认证失败。 */
    AUTHENTICATION_ERROR,

    /** 权限校验失败。 */
    AUTHORIZATION_ERROR,

    /** 网络连接或通信失败。 */
    NETWORK_ERROR,

    /** 等待发送确认超时。 */
    TIMEOUT,

    /** 调用线程在等待确认时被中断。 */
    INTERRUPTED,

    /** Broker 明确拒绝消息。 */
    BROKER_REJECTED,

    /** Provider 客户端本地错误。 */
    CLIENT_ERROR,

    /** 无法进一步分类的错误。 */
    UNKNOWN_ERROR
}
