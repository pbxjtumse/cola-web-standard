package com.xjtu.iron.cola.web.tracing.template;

/**
 * 无返回值的 tracing 回调。
 *
 * <p>主要给 TraceTemplate 使用，用于包装一段无返回值的业务逻辑。</p>
 *
 * <p>类似 Java 的 Runnable，但支持抛出业务异常。</p>
 */
@FunctionalInterface
public interface ITraceRunnable<E extends Throwable> {
    /**
     * 执行业务逻辑。
     *
     * @throws E 业务异常或系统异常
     */
    void run() throws E;
}
