package com.xjtu.iron.transaction.spi.id;

/**
 * 事务逻辑执行 ID 生成扩展点。
 *
 * <p>executionId 不是数据库事务 ID，只用于日志、事件和诊断关联。
 * 默认实现使用本地 UUID；如果工程已有 foundation-id，可通过适配此 SPI 统一 ID 策略，
 * transaction-core 不需要直接依赖雪花、号段、Redis 等具体算法。</p>
 */
@FunctionalInterface
public interface TransactionExecutionIdGenerator {

    String nextId();
}
