package com.xjtu.iron.message.api;

/**
 * 表示一期普通消费闭环中的业务处理决策。
 */
public enum ConsumeDecision {

    /** 业务处理完成，Provider 可以推进确认位置。 */
    SUCCESS,

    /** 当前处理失败，Provider 应触发重新投递或重新消费。 */
    RETRY
}
