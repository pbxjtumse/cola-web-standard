package com.xjtu.iron.relational.api.statement;

/**
 * 已由上层 Storage / Sharding Adapter 确定的数据源路由结果。
 *
 * <p>Relational Access 不计算 shard，也不理解 userId、merchantId 等业务路由键。
 * 上层若启用分库分表，应先计算目标库表；物理表名进入最终 SQL，dataSourceKey 用于
 * DataSourceResolver 选择目标 DataSource。</p>
 *
 * @param dataSourceKey 目标数据源键；null 表示默认数据源
 */
public record SqlRoute(String dataSourceKey) {
}
