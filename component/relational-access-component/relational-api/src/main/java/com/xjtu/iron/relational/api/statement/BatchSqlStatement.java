package com.xjtu.iron.relational.api.statement;

import java.util.List;

/**
 * 同一 SQL、不同参数组的批量执行请求。
 *
 * @param operationName 稳定逻辑操作名
 * @param sql 批量执行的固定 SQL
 * @param batches 每个元素代表一次 addBatch 所需的完整位置参数列表
 * @param options Statement 执行选项
 * @param route 已由上层确定的数据源路由
 */
public record BatchSqlStatement(
        String operationName,
        String sql,
        List<List<SqlParameter>> batches,
        SqlExecutionOptions options,
        SqlRoute route
) {

    public BatchSqlStatement {
        batches = batches == null
                ? List.of()
                : batches.stream().map(List::copyOf).toList();
        options = options == null ? SqlExecutionOptions.defaults() : options;
        route = route == null ? SqlRoute.defaultRoute() : route;
    }

    public static BatchSqlStatement of(
            String operationName,
            String sql,
            List<List<SqlParameter>> batches
    ) {
        return new BatchSqlStatement(
                operationName,
                sql,
                batches,
                SqlExecutionOptions.defaults(),
                SqlRoute.defaultRoute()
        );
    }

    public BatchSqlStatement withOptions(SqlExecutionOptions newOptions) {
        return new BatchSqlStatement(operationName, sql, batches, newOptions, route);
    }

    public BatchSqlStatement withRoute(SqlRoute newRoute) {
        return new BatchSqlStatement(operationName, sql, batches, options, newRoute);
    }
}
