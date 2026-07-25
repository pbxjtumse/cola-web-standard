package com.xjtu.iron.message.api;

/**
 * 表示发送流程结束或失败时所处的生命周期阶段。
 */
public enum SendStage {

    /** 参数、目的地或 Provider 能力校验阶段。 */
    VALIDATE,

    /** 消息体序列化阶段。 */
    SERIALIZE,

    /** 调用具体 Provider 发送阶段。 */
    SEND,

    /** 等待 Broker 或 Provider 确认阶段。 */
    CONFIRM,

    /** 发送流程已完成。 */
    COMPLETE
}
