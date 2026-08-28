package com.xjtu.iron.message.core.consume.transaction;

/**
 * 消息消费事务抽象，包住业务 Handler 和幂等终态更新。
 */
public interface MessageConsumeTransactionExecutor {
    <T> T execute(MessageConsumeTransactionContext context, MessageConsumeTransactionalOperation<T> operation);
}
