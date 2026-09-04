package com.xjtu.iron.relational.spi;

import com.xjtu.iron.relational.api.statement.SqlExecutionOptions;
import com.xjtu.iron.relational.api.statement.SqlRoute;

import java.util.Map;

/**
 * Relational Core 在一次实际 SQL 执行期间传递给 SPI 的上下文。
 *
 * <p>该对象属于基础设施扩展契约，不是业务上下文，也不应携带 orderId、userId 等
 * 高基数业务标签。</p>
 *
 * @param operationName 稳定逻辑操作名
 * @param sql 最终 SQL
 * @param route 已确定的数据源路由
 * @param options Statement 执行选项
 * @param attributes 面向集成扩展的低基数附加属性
 */
public record SqlExecutionContext(
        String operationName,
        String sql,
        SqlRoute route,
        SqlExecutionOptions options,
        Map<String, Object> attributes
) {
}
