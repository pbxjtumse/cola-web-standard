package com.xjtu.iron.cola.web.tracing;

/**
 * @author pangbo
 */
@FunctionalInterface
public interface ITraceRunnable<E extends Throwable> {
    void run() throws E;
}
