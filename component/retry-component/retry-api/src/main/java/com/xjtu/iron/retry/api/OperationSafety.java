package com.xjtu.iron.retry.api;

/**
 * 操作重复执行时的副作用安全级别。
 *
 * <p>该枚举只用于表达和观测，不会替代真正的业务幂等机制。</p>
 */
public enum OperationSafety {

    /**
     * 调用方没有声明操作安全性。
     */
    UNSPECIFIED,

    /**
     * 只读操作，通常可以安全重复执行。
     */
    READ_ONLY,

    /**
     * 操作自身具有天然幂等性。
     */
    IDEMPOTENT,

    /**
     * 操作已经被外部幂等组件或业务唯一键保护。
     */
    IDEMPOTENCY_PROTECTED,

    /**
     * 操作不是幂等的，重复执行可能产生额外副作用。
     */
    NON_IDEMPOTENT
}
