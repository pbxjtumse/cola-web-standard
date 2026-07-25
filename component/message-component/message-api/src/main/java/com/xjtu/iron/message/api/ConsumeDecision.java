package com.xjtu.iron.message.api;

/**
 * 表示业务 Handler 对当前消息的基础消费决策。
 *
 * <p>第一版只统一成功确认与失败重投两个最小语义。</p>
 */
public enum ConsumeDecision {

    /** 业务处理完成，Provider 可以确认或提交消费进度。 */
    SUCCESS,

    /** 当前处理失败，Provider 不应推进消费进度并应触发重新投递。 */
    RETRY
}
