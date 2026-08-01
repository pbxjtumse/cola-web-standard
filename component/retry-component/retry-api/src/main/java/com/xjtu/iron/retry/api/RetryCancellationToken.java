package com.xjtu.iron.retry.api;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 提供进程内同步重试的协作式取消信号。
 *
 * <p>取消令牌不能强制终止正在运行的业务代码，只能阻止下一次尝试或下一次等待。</p>
 */
@FunctionalInterface
public interface RetryCancellationToken {

    /** 永不取消的共享令牌。 */
    RetryCancellationToken NONE = () -> false;

    boolean isCancellationRequested();

    /** 返回永不取消的默认令牌。 */
    static RetryCancellationToken none() {
        return NONE;
    }

    /** 基于 AtomicBoolean 构造一个线程安全取消令牌。 */
    static RetryCancellationToken from(AtomicBoolean cancelled) {
        if (cancelled == null) {
            throw new IllegalArgumentException("cancelled must not be null");
        }
        return cancelled::get;
    }
}
