package com.xjtu.iron.relational.api.statement;

import java.sql.JDBCType;
import java.util.Objects;

/**
 * 单个 JDBC 位置参数。
 *
 * @param value 参数值，可为 null
 * @param jdbcType 显式 JDBC 类型；为 null 时由后续执行实现推断
 * @param sensitive 是否为敏感参数；后续日志/观测实现应据此避免明文输出
 */
public record SqlParameter(
        Object value,
        JDBCType jdbcType,
        boolean sensitive
) {

    public static SqlParameter of(Object value) {
        return new SqlParameter(value, null, false);
    }

    public static SqlParameter of(Object value, JDBCType jdbcType) {
        return new SqlParameter(value, jdbcType, false);
    }

    public static SqlParameter nullOf(JDBCType jdbcType) {
        return new SqlParameter(null, Objects.requireNonNull(jdbcType, "jdbcType"), false);
    }

    public static SqlParameter sensitive(Object value) {
        return new SqlParameter(value, null, true);
    }

    public static SqlParameter sensitive(Object value, JDBCType jdbcType) {
        return new SqlParameter(value, jdbcType, true);
    }
}
