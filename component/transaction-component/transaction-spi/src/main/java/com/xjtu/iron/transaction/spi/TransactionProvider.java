package com.xjtu.iron.transaction.spi;

import com.xjtu.iron.transaction.api.TransactionOptions;

/**
 * 底层事务基础设施适配 SPI。
 *
 * <p>这里故意不暴露 begin/commit/rollback/suspend/resume 五六个低层方法。
 * 这些动作属于具体事务管理器自身的职责，例如 Spring PlatformTransactionManager。
 * 组件只要求 Provider 按 TransactionOptions 完成一个完整事务回调。</p>
 */
public interface TransactionProvider {

    <T> TransactionProviderResult<T> execute(
            TransactionOptions options,
            TransactionProviderCallback<T> callback);
}
