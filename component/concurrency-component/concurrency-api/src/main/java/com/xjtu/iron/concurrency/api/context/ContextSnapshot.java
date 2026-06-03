package com.xjtu.iron.concurrency.api.context;

/**
 * 上下文快照。
 *
 * <p>表示从提交任务线程捕获到的一份上下文。</p>
 */
public interface ContextSnapshot {

    /**
     * 在当前线程恢复上下文。
     *
     * @return 上下文作用域，close 时恢复旧上下文
     */
    ContextScope restore();
}
