package com.xjtu.iron.message.core.context;

import java.util.Optional;

/**
 * 定义当前入站消息上下文的访问和作用域契约。
 */
public interface MessageContextAccessor {

    /**
     * 返回当前线程正在处理的入站消息。
     *
     * @return 当前消息；不存在时为空
     */
    Optional<CurrentMessage> current();

    /**
     * 打开一个当前消息作用域。
     *
     * @param currentMessage 当前入站消息
     * @return 关闭后恢复上一个上下文的作用域
     */
    Scope open(CurrentMessage currentMessage);

    /**
     * 表示可关闭的上下文作用域。
     */
    @FunctionalInterface
    interface Scope extends AutoCloseable {

        /**
         * 关闭当前作用域并恢复上一个上下文。
         */
        @Override
        void close();
    }
}
