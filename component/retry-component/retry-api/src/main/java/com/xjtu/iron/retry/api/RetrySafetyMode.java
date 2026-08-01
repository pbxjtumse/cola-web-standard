package com.xjtu.iron.retry.api;

/** 定义非幂等操作配置多次尝试时的处理方式。 */
public enum RetrySafetyMode {
    /** 允许配置，不额外发布安全告警。 */
    ALLOW,
    /** 允许配置，同时发布安全告警事件。 */
    WARN,
    /** 在策略构建阶段直接拒绝不安全配置。 */
    REJECT
}
