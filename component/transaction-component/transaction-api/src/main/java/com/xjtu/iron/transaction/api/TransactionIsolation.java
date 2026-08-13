package com.xjtu.iron.transaction.api;

/**
 * 事务隔离级别。
 *
 * <p>组件只表达统一语义；最终是否被具体数据库/ORM/事务管理器支持，
 * 由底层 Provider 决定，组件不会伪造支持。</p>
 */
public enum TransactionIsolation {
    DEFAULT,
    READ_UNCOMMITTED,
    READ_COMMITTED,
    REPEATABLE_READ,
    SERIALIZABLE
}
