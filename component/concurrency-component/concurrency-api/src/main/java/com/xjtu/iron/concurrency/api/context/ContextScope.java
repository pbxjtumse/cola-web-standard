package com.xjtu.iron.concurrency.api.context;

/**
 * 上下文作用域。
 *
 * <p>异步任务执行完成后，需要 close，恢复线程原有上下文。</p>
 */
public interface ContextScope extends AutoCloseable {

    @Override
    void close();
}
