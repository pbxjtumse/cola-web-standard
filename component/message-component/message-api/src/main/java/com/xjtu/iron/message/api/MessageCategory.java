package com.xjtu.iron.message.api;

/**
 * 描述逻辑消息目的地的业务语义类别。
 *
 * <p>类别只表达业务意图，不直接决定底层 Topic、Queue、Partition 或 Subscription。</p>
 */
public enum MessageCategory {

    /**
     * 表示已经发生的业务事实，例如订单已支付。【表示事实】
     */
    EVENT,

    /**
     * 表示期望某个接收方执行动作，例如创建支付单。【表示要求】
     */
    COMMAND,

    /**
     * 表示面向通知、推送或告知场景的消息。
     */
    NOTIFICATION
}
