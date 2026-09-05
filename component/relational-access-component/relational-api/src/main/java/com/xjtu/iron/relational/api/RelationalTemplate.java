package com.xjtu.iron.relational.api;

import com.xjtu.iron.relational.api.mapping.RowMapper;
import com.xjtu.iron.relational.api.result.BatchResult;
import com.xjtu.iron.relational.api.result.GeneratedKey;
import com.xjtu.iron.relational.api.result.UpdateResult;
import com.xjtu.iron.relational.api.statement.BatchSqlStatement;
import com.xjtu.iron.relational.api.statement.SqlStatement;

import java.util.List;
import java.util.Optional;

/**
 * 关系型数据库访问的稳定门面。
 *
 * <p>该接口主要供技术组件的 JDBC Storage Adapter 使用，例如：
 * JdbcIdempotencyStorage、JdbcOutboxStorage、JdbcTaskStorage。</p>
 *
 * <p>调用方负责提供明确 SQL、参数和结果映射；Relational Access 负责后续统一的
 * Connection 获取、Statement 生命周期、参数绑定、异常翻译和可观测执行。</p>
 *
 * <p>本接口不承担 ORM、业务 Repository、事务边界、分库分表算法和自动重试。</p>
 */
public interface RelationalTemplate {

    /**
     * 查询至多一行数据。
     *
     * <p>0 行返回 Optional.empty()；1 行由 RowMapper 映射；超过 1 行应由实现层
     * 转换为 NON_UNIQUE_RESULT 类型的 RelationalAccessException。</p>
     */
    <T> Optional<T> queryOne(SqlStatement statement, RowMapper<T> rowMapper);

    /**
     * 查询多行数据并逐行映射。
     */
    <T> List<T> queryList(SqlStatement statement, RowMapper<T> rowMapper);

    /**
     * 查询单个标量值，例如 COUNT(*)、MAX(id) 或单列状态值。
     */
    <T> Optional<T> queryScalar(SqlStatement statement, Class<T> requiredType);

    /**
     * 执行 INSERT / UPDATE / DELETE 等更新操作。
     */
    UpdateResult update(SqlStatement statement);

    /**
     * 执行插入并读取数据库生成键。
     *
     * <p>Generated Key 是兼容能力，并不意味着基础组件推荐使用数据库自增主键。</p>
     */
    <K> GeneratedKey<K> insertAndReturnKey(SqlStatement statement, Class<K> keyType);

    /**
     * 对同一 SQL 的多组参数执行 JDBC batch。
     */
    BatchResult batchUpdate(BatchSqlStatement statement);
}
