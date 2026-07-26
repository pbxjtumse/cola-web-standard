package com.xjtu.iron.message.api;

/**
 * 表示发送调用最终能够确认的结果状态。
 */
public enum SendStatus {

    /** Broker 或 Provider 已明确确认接收成功。 */
    CONFIRMED,

    /** 已明确失败，并且当前调用没有进入成功确认状态。 */
    FAILED,

    /** 配置、权限、路由或 Broker 规则明确拒绝消息。 */
    REJECTED,

    /** 无法确认 Broker 最终是否接收，直接重发可能产生重复消息。 */
    UNKNOWN
}
