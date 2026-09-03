package com.xjtu.iron.message.core.consume.transaction;

/**
 * 消息消费事务抽象。
 *
 * <p>该接口只负责包住一次消费执行回调。是否使用 Spring 本地事务、
 * 自研 transaction-component，或者直接 no-op，由具体实现决定。</p>
 */
public interface MessageConsumeTransactionExecutor {
    <T> T execute(MessageConsumeTransactionContext context, MessageConsumeTransactionalOperation<T> operation);
}
