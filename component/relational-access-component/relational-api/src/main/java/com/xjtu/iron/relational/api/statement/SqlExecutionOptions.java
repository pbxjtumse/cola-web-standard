package com.xjtu.iron.relational.api.statement;

import java.time.Duration;

/**
 * 单次 SQL 执行选项。
 *
 * <p>null 表示使用组件默认配置。这里不描述事务超时和业务超时，只描述 JDBC Statement
 * 层面的执行提示。</p>
 */
public record SqlExecutionOptions(
        Duration timeout,
        Integer fetchSize,
        Integer maxRows
) {

    private static final SqlExecutionOptions DEFAULTS = new SqlExecutionOptions(null, null, null);

    public static SqlExecutionOptions defaults() {
        return DEFAULTS;
    }

    public static SqlExecutionOptions timeout(Duration timeout) {
        return new SqlExecutionOptions(timeout, null, null);
    }
}
