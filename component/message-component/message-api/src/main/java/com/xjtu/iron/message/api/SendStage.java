package com.xjtu.iron.message.api;

/**
 * 描述发送生命周期中产生结果或失败的阶段。
 */
public enum SendStage {

    /** 校验消息、目的地和发送选项。 */
    VALIDATE,

    /** 补齐消息 ID、时间和上下文。 */
    ENRICH,

    /** 将逻辑目的地解析为 Provider 和物理目的地。 */
    RESOLVE,

    /** 将业务消息体序列化为字节。 */
    SERIALIZE,

    /** 调用 Provider 客户端发起发送。 */
    SEND,

    /** 等待或处理 Broker 发送确认。 */
    CONFIRM,

    /** 由可靠发送层执行 retry-component 编排。 */
    RETRY,

    /** 发送生命周期已经完成。 */
    COMPLETE
}
