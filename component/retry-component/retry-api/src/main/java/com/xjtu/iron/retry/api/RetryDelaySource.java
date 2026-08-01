package com.xjtu.iron.retry.api;

/** 描述一次退避等待时间的来源。 */
public enum RetryDelaySource {
    /** 不等待。 */
    NONE,
    /** 固定等待。 */
    FIXED,
    /** 指数退避。 */
    EXPONENTIAL,
    /** 指数退避基础上的全抖动。 */
    FULL_JITTER,
    /** 根据失败类别选择了委托策略。 */
    CATEGORY_AWARE,
    /** 下游通过 Retry-After 等协议明确指定。 */
    SERVER_DIRECTED,
    /** 调用方自定义决策或策略提供。 */
    CUSTOM
}
