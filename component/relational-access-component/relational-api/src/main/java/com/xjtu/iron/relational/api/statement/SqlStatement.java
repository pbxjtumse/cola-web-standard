package com.xjtu.iron.relational.api.statement;

import java.util.Arrays;
import java.util.List;

/**
 * 一次确定的 SQL 执行请求。
 *
 * @param operationName 稳定、低基数的逻辑操作名，例如 idempotency.try-acquire；用于指标和日志
 * @param sql 已经由上层 Storage Adapter 确定的 SQL；Relational Access 不负责理解领域语义
 * @param parameters 按 ? 占位符顺序排列的位置参数
 * @param options 本次 Statement 执行选项
 * @param route 已由上层确定的数据源路由；Relational Access 不负责计算 shard
 */
public record SqlStatement(
        String operationName,
        String sql,
        List<SqlParameter> parameters,
        SqlExecutionOptions options,
        SqlRoute route
) {

    public SqlStatement {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        options = options == null ? SqlExecutionOptions.defaults() : options;
        route = route == null ? SqlRoute.defaultRoute() : route;
    }

    /**
     * 最常用的快捷构造：普通参数自动包装为 SqlParameter。
     */
    public static SqlStatement of(String operationName, String sql, Object... parameters) {
        List<SqlParameter> sqlParameters = parameters == null
                ? List.of()
                : Arrays.stream(parameters).map(SqlParameter::of).toList();
        return new SqlStatement(
                operationName,
                sql,
                sqlParameters,
                SqlExecutionOptions.defaults(),
                SqlRoute.defaultRoute()
        );
    }

    public SqlStatement withOptions(SqlExecutionOptions newOptions) {
        return new SqlStatement(operationName, sql, parameters, newOptions, route);
    }

    public SqlStatement withRoute(SqlRoute newRoute) {
        return new SqlStatement(operationName, sql, parameters, options, newRoute);
    }
}
