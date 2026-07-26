package com.xjtu.iron.message.spi;

/**
 * 表示具体 Provider 已经启动的消费订阅。
 */
@FunctionalInterface
public interface ProviderSubscription extends AutoCloseable {

    /**
     * 停止消费并释放 Provider 资源。
     */
    @Override
    void close();
}
