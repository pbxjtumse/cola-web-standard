package com.xjtu.iron.relational.api.statement;

import java.util.List;

/**
 * 同一 SQL、不同参数组的批量执行请求。
 *
 * @param operationName 稳定逻辑操作名
 * @param sql 批量执行的固定 SQL
 * @param batches 每个元素代表一次 addBatch 所需的完整位置参数列表
 * @param options Statement 执行选项
 */
public record BatchSqlStatement(
        String operationName,
        String sql,
        List<List<SqlParameter>> batches,
        SqlExecutionOptions options
) {
}
