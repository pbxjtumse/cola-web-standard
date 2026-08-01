package com.xjtu.iron.retry.api;

/**
 * 描述一个业务操作在被重复执行时的副作用安全级别。
 *
 * <p>该声明只用于策略校验、告警和观测，不会自动实现业务幂等。</p>
 */
public enum OperationSafety {
    /** 调用方没有声明操作的重复执行安全性。 */
    UNSPECIFIED,
    /** 只读操作，通常可以安全地重复执行。 */
    READ_ONLY,
    /** 操作本身具有天然幂等性。 */
    IDEMPOTENT,
    /** 操作已经由业务唯一键、条件更新或幂等组件保护。 */
    IDEMPOTENCY_PROTECTED,
    /** 重复执行可能产生重复扣款、重复发券等额外副作用。 */
    NON_IDEMPOTENT
}
