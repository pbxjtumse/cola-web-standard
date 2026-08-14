package com.xjtu.iron.transaction.spi.provider;

import com.xjtu.iron.transaction.api.definition.TransactionOptions;

/**
 * 底层事务基础设施适配 SPI。
 *
 * <p>SPI 不把 begin/commit/rollback/suspend/resume 拆成低层方法，
 * 避免 core 重新实现一套事务管理器；这些动作由具体 Provider 负责。</p>
 */
public interface TransactionProvider {

    <T> TransactionProviderResult<T> execute(TransactionOptions options, TransactionProviderCallback<T> callback);
}
