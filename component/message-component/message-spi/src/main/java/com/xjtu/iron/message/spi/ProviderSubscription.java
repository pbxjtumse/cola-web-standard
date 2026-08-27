package com.xjtu.iron.message.spi;

/**
 * Provider 层订阅句柄。
 *
 * <p>它屏蔽了不同中间件关闭订阅的细节，message-core 最终会把它适配为 API 层的 {@code MessageSubscription}。</p>
 */
@FunctionalInterface
public interface ProviderSubscription extends AutoCloseable {

    /**
     * 停止消费并释放 Provider 资源。
     */
    @Override
    void close();
}
