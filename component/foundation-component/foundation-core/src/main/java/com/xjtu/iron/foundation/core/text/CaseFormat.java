package com.xjtu.iron.foundation.core.text;

/**
 * 定义组件工程支持的常用命名格式。
 */
public enum CaseFormat {

    /** 小驼峰格式，例如 {@code retryPolicy}。 */
    LOWER_CAMEL,

    /** 大驼峰格式，例如 {@code RetryPolicy}。 */
    UPPER_CAMEL,

    /** 小写下划线格式，例如 {@code retry_policy}。 */
    LOWER_UNDERSCORE,

    /** 大写下划线格式，例如 {@code RETRY_POLICY}。 */
    UPPER_UNDERSCORE,

    /** 小写短横线格式，例如 {@code retry-policy}。 */
    LOWER_HYPHEN
}
