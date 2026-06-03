package com.xjtu.iron.concurrency.api.context;

/**
 * 上下文传播器。
 *
 * <p>用于从当前线程捕获上下文。</p>
 */
public interface ContextPropagator {

    /**
     * 捕获当前线程上下文。
     *
     * @return 上下文快照
     */
    ContextSnapshot capture();
}