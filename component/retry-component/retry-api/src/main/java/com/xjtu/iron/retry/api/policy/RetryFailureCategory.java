package com.xjtu.iron.retry.api.policy;

/** 为不同协议的失败提供统一、稳定的语义分类。 */
public enum RetryFailureCategory {
    /** 尚未识别或不适合归类。 */
    UNKNOWN,
    /** 网络抖动、连接重置等瞬时故障。 */
    TRANSIENT,
    /** 下游限流、过载或明确要求稍后重试。 */
    THROTTLING,
    /** 乐观锁、死锁或条件更新冲突。 */
    CONCURRENCY_CONFLICT,
    /** 下游服务、消息代理或数据库暂时不可用。 */
    DEPENDENCY_UNAVAILABLE,
    /** 操作没有抛出异常，但业务结果仍处于处理中。 */
    RESULT_NOT_READY,
    /** 参数错误、权限错误或业务规则拒绝等永久性失败。 */
    NON_RETRYABLE
}
