package com.xjtu.iron.message.api.consume;

/**
 * 表示一个已经启动的消费订阅关闭句柄。
 */
@FunctionalInterface
public interface MessageSubscription extends AutoCloseable {

    /**
     * 停止消费并释放相关 Provider 资源。
     */
    @Override
    void close();
}
