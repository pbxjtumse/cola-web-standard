package com.xjtu.iron.transaction.core.id;

import com.xjtu.iron.transaction.spi.id.TransactionExecutionIdGenerator;

import java.util.UUID;

/**
 * 一期默认 executionId 生成器。
 *
 * <p>纯本地、无远程依赖；后续可被 foundation-id 适配实现替换。</p>
 */
public final class UuidTransactionExecutionIdGenerator implements TransactionExecutionIdGenerator {

    @Override
    public String nextId() {
        // executionId 只承担一次事务模板调用的关联标识，不要求连续，也不访问远程号段服务。
        return UUID.randomUUID().toString();
    }
}
