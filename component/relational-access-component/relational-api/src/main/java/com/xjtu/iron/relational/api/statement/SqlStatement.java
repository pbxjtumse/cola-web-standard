package com.xjtu.iron.relational.api.statement;

import java.util.List;

/**
 * 一次确定的 SQL 执行请求。
 *
 * @param operationName 稳定、低基数的逻辑操作名，例如 idempotency.try-acquire；用于指标和日志
 * @param sql 已经由上层 Storage Adapter 确定的 SQL；Relational Access 不负责理解领域语义
 * @param parameters 按 ? 占位符顺序排列的位置参数
 * @param options 本次 Statement 执行选项
 */
public record SqlStatement(
        String operationName,
        String sql,
        List<SqlParameter> parameters,
        SqlExecutionOptions options
) {
}
