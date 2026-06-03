package com.xjtu.iron.concurrency.api.context;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * 上下文感知任务装饰器。
 *
 * <p>用于包装 Runnable / Callable / Supplier，使异步任务执行时能恢复提交线程的上下文。</p>
 */
public interface ContextAwareTaskDecorator {

    Runnable decorate(Runnable runnable);

    <T> Callable<T> decorate(Callable<T> callable);

    <T> Supplier<T> decorate(Supplier<T> supplier);
}