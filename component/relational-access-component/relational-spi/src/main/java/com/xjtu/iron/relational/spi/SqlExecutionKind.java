package com.xjtu.iron.relational.spi;

/**
 * 一次实际关系型访问的执行种类。
 *
 * <p>用于 SPI、指标和日志描述执行行为，不承担 SQL 解析。</p>
 */
public enum SqlExecutionKind {
    QUERY_ONE,
    QUERY_LIST,
    QUERY_SCALAR,
    UPDATE,
    INSERT_WITH_GENERATED_KEY,
    BATCH_UPDATE
}
