package com.xjtu.iron.message.api;

/**
 * 表示已经启动的消息消费者句柄。
 */
public interface MessageConsumer extends AutoCloseable {

    /**
     * 停止消费者并释放相关资源。
     */
    @Override
    void close();
}
