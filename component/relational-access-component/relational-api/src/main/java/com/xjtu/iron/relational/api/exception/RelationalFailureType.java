package com.xjtu.iron.relational.api.exception;

/**
 * 面向上层 Storage / Retry / Transaction 的稳定关系型失败分类。
 *
 * <p>不同数据库的 SQLState、vendorCode 最终应由 SqlExceptionTranslator 投影到该枚举，
 * 从而避免上层技术组件直接绑定 MySQL/PostgreSQL/Oracle 的具体错误码。</p>
 */
public enum RelationalFailureType {

    CONNECTION_ERROR,
    TIMEOUT,
    DEADLOCK,
    LOCK_TIMEOUT,
    DUPLICATE_KEY,
    CONSTRAINT_VIOLATION,
    SERIALIZATION_FAILURE,
    SQL_SYNTAX_ERROR,
    DATA_ERROR,
    NON_UNIQUE_RESULT,
    RESULT_MAPPING_ERROR,
    GENERATED_KEY_UNAVAILABLE,
    UNKNOWN
}
