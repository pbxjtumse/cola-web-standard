package com.xjtu.iron.message.core.consume.transaction;

/**
 * 未启用事务模板时的默认执行器。
 */
public final class NoopMessageConsumeTransactionExecutor implements MessageConsumeTransactionExecutor {
    @Override
    public <T> T execute(MessageConsumeTransactionContext context, MessageConsumeTransactionalOperation<T> operation) {
        return operation.execute();
    }
}
