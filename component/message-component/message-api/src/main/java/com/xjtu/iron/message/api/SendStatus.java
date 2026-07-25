package com.xjtu.iron.message.api;

/**
 * 表示消息发送的最终确认状态。
 */
public enum SendStatus {

    /** Provider 已明确确认消息发送成功。 */
    CONFIRMED,

    /** Provider 已明确确认消息未发送成功。 */
    FAILED,

    /** Broker 或 Provider 明确拒绝消息。 */
    REJECTED,

    /** 因超时或链路中断而无法判断 Broker 是否已经接收消息。 */
    UNKNOWN
}
